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
import com.assignment.phoneinventory.search.ElasticsearchIndexer;
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

@Service
public class TelephoneService {

    private final TelephoneNumberDao telDao;
    private final AuditLogDao auditDao;
    private final ElasticsearchIndexer indexer;

    public TelephoneService(TelephoneNumberDao telDao, AuditLogDao auditDao, ElasticsearchIndexer indexer) {
        this.telDao = telDao;
        this.auditDao = auditDao;
        this.indexer = indexer;
    }

    public PageResponse<TelephoneNumber> search(String cc, String ac, String contains, TelephoneNumber.Status status, int page, int size) {
        java.util.List<TelephoneNumber> rows = telDao.search(cc, ac, contains, status == null ? null : status.name(), page, size);
        long count = telDao.count(cc, ac, contains, status == null ? null : status.name());
        return new PageResponse<>(rows, page, size, count);
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
        TelephoneNumber result = getOrThrow(number);
        indexer.index(result);
        return result;
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
        TelephoneNumber result = getOrThrow(number);
        indexer.index(result);
        return result;
    }

    @Transactional
    public TelephoneNumber activate(String number, String userId) {
        TelephoneNumber tn = getOrThrow(number);
        if (tn.getStatus() != TelephoneNumber.Status.ALLOCATED) {
            throw new BusinessRuleViolationException("Only ALLOCATED numbers can be activated");
        }
        if (tn.getAllocatedUserId() == null || !tn.getAllocatedUserId().equals(userId)) {
            throw new BusinessRuleViolationException("Number allocated to a different user");
        }
        TelephoneNumber next = cloneState(tn);
        next.setStatus(TelephoneNumber.Status.ACTIVATED);
        int updated = telDao.updateWithVersion(tn.getId(), tn.getVersion(), next);
        if (updated == 0) {
            throw new IllegalStateException("Concurrent update detected");
        }
        writeAudit(tn, next, userId, "Activated");
        TelephoneNumber result = getOrThrow(number);
        indexer.index(result);
        return result;
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
        TelephoneNumber result = getOrThrow(number);
        indexer.index(result);
        return result;
    }

    private static TelephoneNumber cloneState(TelephoneNumber src) {
        TelephoneNumber t = new TelephoneNumber();
        t.setId(src.getId());
        t.setNumber(src.getNumber());
        t.setCountryCode(src.getCountryCode());
        t.setAreaCode(src.getAreaCode());
        t.setStatus(src.getStatus());
        t.setAllocatedUserId(src.getAllocatedUserId());
        t.setReservedUntil(src.getReservedUntil());
        t.setVersion(src.getVersion());
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
                // Expect columns: number,countryCode,areaCode
                String[] parts = line.split(",");
                if (parts.length != 3) {
                    throw new InvalidCsvFormatException("Expected columns: number,countryCode,areaCode");
                }
                String number = parts[0].trim();
                String cc = parts[1].trim();
                String ac = parts[2].trim();
                if (number.isEmpty() || cc.isEmpty() || ac.isEmpty()) {
                    throw new InvalidCsvFormatException("Missing required value at line " + processed.get());
                }

                try {
                    inserted.addAndGet(telDao.upsertNumber(number, cc, ac));
                } catch (Exception ignore) { }
            }
        } catch (InvalidCsvFormatException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCsvFormatException("Error reading CSV file: " + e.getMessage());
        }
        long proc = processed.get();
        long ins = inserted.get();
        long skipped = proc - ins;
        indexer.reindexAll();
        return new UploadResult(proc, ins, skipped);
    }
}
