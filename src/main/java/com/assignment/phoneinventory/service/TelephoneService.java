package com.assignment.phoneinventory.service;

import com.assignment.phoneinventory.dao.AuditLogDao;
import com.assignment.phoneinventory.dao.TelephoneNumberDao;
import com.assignment.phoneinventory.domain.AuditLog;
import com.assignment.phoneinventory.domain.TelephoneNumber;
import com.assignment.phoneinventory.dto.PageResponse;
import com.assignment.phoneinventory.dto.UploadResult;
import com.assignment.phoneinventory.exception.BusinessRuleViolationException;
import com.assignment.phoneinventory.exception.InvalidCsvFormatException;
import com.assignment.phoneinventory.exception.ResourceNotFoundException;
import com.assignment.phoneinventory.search.TelephoneDocument;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class TelephoneService {

    private final TelephoneNumberDao telDao;
    private final AuditLogDao auditDao;
    private final ElasticsearchOperations esOperations;

    public TelephoneService(TelephoneNumberDao telDao, AuditLogDao auditDao, ElasticsearchOperations esOperations) {
        this.telDao = telDao;
        this.auditDao = auditDao;
        this.esOperations = esOperations;
    }

    public PageResponse<TelephoneNumber> search(String cc, String ac, String prefix, String contains, TelephoneNumber.Status status, int page, int size) {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        if (cc != null) {
            bool.must(QueryBuilders.termQuery("countryCode", cc));
        }
        if (ac != null) {
            bool.must(QueryBuilders.termQuery("areaCode", ac));
        }
        if (prefix != null) {
            bool.must(QueryBuilders.termQuery("prefix", prefix));
        }
        if (contains != null) {
            bool.must(QueryBuilders.wildcardQuery("number", "*" + contains + "*"));
        }
        if (status != null) {
            bool.must(QueryBuilders.termQuery("status", status.name()));
        }

        NativeSearchQueryBuilder builder = new NativeSearchQueryBuilder()
                .withQuery(bool)
                .withPageable(PageRequest.of(page, size));
        SearchHits<TelephoneDocument> hits = esOperations.search(builder.build(), TelephoneDocument.class);
        List<TelephoneNumber> rows = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toDomain)
                .collect(Collectors.toList());
        return new PageResponse<>(rows, page, size, hits.getTotalHits());
    }

    private TelephoneNumber getOrThrow(String number) {
        Optional<TelephoneNumber> opt = telDao.findByNumber(number);
        return opt.orElseThrow(() -> new ResourceNotFoundException("Number " + number + " not found"));
    }

    private void writeAudit(TelephoneNumber before, TelephoneNumber after, String userId, String note) {
        AuditLog log = new AuditLog();
        log.setNumberId(after.getId());
        log.setFromState(before == null || before.getStatus() == null ? null : before.getStatus().name());
        log.setToState(after.getStatus() == null ? null : after.getStatus().name());
        log.setUserId(userId);
        log.setTimestamp(Instant.now());
        log.setNote(note);
        auditDao.insert(log);
    }

    @Transactional
    public TelephoneNumber reserve(String number, String userId, Duration hold) {
        TelephoneNumber tn = getOrThrow(number);
        if (tn.getStatus() != TelephoneNumber.Status.AVAILABLE) {
            throw new BusinessRuleViolationException("Only AVAILABLE numbers can be reserved");
        }
        TelephoneNumber next = cloneState(tn);
        next.setStatus(TelephoneNumber.Status.RESERVED);
        next.setAllocatedUserId(userId);
        next.setReservedUntil(Instant.now().plus(hold));
        int updated = telDao.updateWithVersion(tn.getId(), tn.getVersion(), next);
        if (updated == 0) {
            throw new IllegalStateException("Concurrent update detected");
        }
        writeAudit(tn, next, userId, "Reserved for " + hold.toMinutes() + " min");
        return getOrThrow(number);
    }

    @Transactional
    public TelephoneNumber allocate(String number, String userId) {
        TelephoneNumber tn = getOrThrow(number);
        if (tn.getReservedUntil() != null && tn.getReservedUntil().isBefore(Instant.now())) {
            throw new IllegalStateException("Reservation expired");
        }
        if (tn.getStatus() != TelephoneNumber.Status.RESERVED) {
            throw new BusinessRuleViolationException("Only RESERVED numbers can be allocated");
        }
        if (tn.getAllocatedUserId() == null || !tn.getAllocatedUserId().equals(userId)) {
            throw new BusinessRuleViolationException("Number reserved for a different user");
        }
        TelephoneNumber next = cloneState(tn);
        next.setStatus(TelephoneNumber.Status.ALLOCATED);
        // keep allocatedUserId; clear reservedUntil
        next.setReservedUntil(null);
        int updated = telDao.updateWithVersion(tn.getId(), tn.getVersion(), next);
        if (updated == 0) {
            throw new IllegalStateException("Concurrent update detected");
        }
        writeAudit(tn, next, userId, "Allocated");
        return getOrThrow(number);
    }

    @Transactional
    public TelephoneNumber activate(String number, String userId) {
        TelephoneNumber tn = getOrThrow(number);
        if (tn.getStatus() != TelephoneNumber.Status.ALLOCATED) {
            throw new BusinessRuleViolationException("Only ALLOCATED numbers can be activated");
        }
        TelephoneNumber next = cloneState(tn);
        next.setStatus(TelephoneNumber.Status.ACTIVATED);
        int updated = telDao.updateWithVersion(tn.getId(), tn.getVersion(), next);
        if (updated == 0) {
            throw new IllegalStateException("Concurrent update detected");
        }
        writeAudit(tn, next, userId, "Activated");
        return getOrThrow(number);
    }

    @Transactional
    public TelephoneNumber deactivate(String number, String userId) {
        TelephoneNumber tn = getOrThrow(number);
        if (tn.getStatus() != TelephoneNumber.Status.ACTIVATED) {
            throw new BusinessRuleViolationException("Only ACTIVATED numbers can be deactivated");
        }
        TelephoneNumber next = cloneState(tn);
        next.setStatus(TelephoneNumber.Status.DEACTIVATED);
        int updated = telDao.updateWithVersion(tn.getId(), tn.getVersion(), next);
        if (updated == 0) {
            throw new IllegalStateException("Concurrent update detected");
        }
        writeAudit(tn, next, userId, "Deactivated");
        return getOrThrow(number);
    }

    private static TelephoneNumber cloneState(TelephoneNumber src) {
        TelephoneNumber t = new TelephoneNumber();
        t.setId(src.getId());
        t.setNumber(src.getNumber());
        t.setCountryCode(src.getCountryCode());
        t.setAreaCode(src.getAreaCode());
        t.setPrefix(src.getPrefix());
        t.setStatus(src.getStatus());
        t.setAllocatedUserId(src.getAllocatedUserId());
        t.setReservedUntil(src.getReservedUntil());
        t.setVersion(src.getVersion());
        t.setNumberDigits(src.getNumberDigits());
        return t;
    }

    private TelephoneNumber toDomain(TelephoneDocument doc) {
        TelephoneNumber t = new TelephoneNumber();
        t.setId(doc.getId());
        t.setNumber(doc.getNumber());
        t.setCountryCode(doc.getCountryCode());
        t.setAreaCode(doc.getAreaCode());
        t.setPrefix(doc.getPrefix());
        if (doc.getStatus() != null) {
            t.setStatus(TelephoneNumber.Status.valueOf(doc.getStatus()));
        }
        t.setAllocatedUserId(doc.getAllocatedUserId());
        t.setReservedUntil(doc.getReservedUntil());
        t.setVersion(doc.getVersion());
        t.setNumberDigits(doc.getNumberDigits());
        return t;
    }

    // Optional: CSV ingest via service (not used by controller; kept compile-safe)
    public UploadResult importCsv(InputStream in) {
        AtomicLong processed = new AtomicLong();
        AtomicLong inserted = new AtomicLong();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                processed.incrementAndGet();
                // Expect columns: number,countryCode,areaCode,prefix
                String[] parts = line.split(",");
                if (parts.length < 4) continue;
                String number = parts[0].trim();
                String cc = parts[1].trim();
                String ac = parts[2].trim();
                String pref = parts[3].trim();
                try {
                    inserted.addAndGet(telDao.upsertNumber(number, cc, ac, pref));
                } catch (Exception ignore) { }
            }
        } catch (Exception e) {
            throw new InvalidCsvFormatException("Error reading CSV file: " + e.getMessage());
        }
        long proc = processed.get();
        long ins = inserted.get();
        long skipped = proc - ins;
        return new UploadResult(proc, ins, skipped);
    }
}
