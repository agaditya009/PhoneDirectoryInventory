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
import com.assignment.phoneinventory.constants.CommonConstants;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
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
    private final ElasticsearchOperations esOps;

    public TelephoneService(TelephoneNumberDao telDao, AuditLogDao auditDao, ElasticsearchOperations esOps) {
        this.telDao = telDao;
        this.auditDao = auditDao;
        this.esOps = esOps;
    }

    public PageResponse<TelephoneNumber> search(String cc, String ac, String contains, TelephoneNumber.Status status, int page, int size) {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        if (cc != null) bool.must(QueryBuilders.termQuery("countryCode", cc));
        if (ac != null) bool.must(QueryBuilders.termQuery("areaCode", ac));
        if (status != null) bool.must(QueryBuilders.termQuery("status", status.name()));
        if (contains != null && !contains.isBlank()) bool.must(QueryBuilders.wildcardQuery("number", "*" + contains + "*"));

        Query query = new NativeSearchQueryBuilder()
                .withQuery(bool)
                .withPageable(PageRequest.of(page, size))
                .build();

        SearchHits<TelephoneDocument> hits = esOps.search(query, TelephoneDocument.class);
        List<TelephoneNumber> rows = new ArrayList<>();
        for (SearchHit<TelephoneDocument> hit : hits) {
            TelephoneDocument doc = hit.getContent();
            TelephoneNumber tn = new TelephoneNumber();
            tn.setNumber(doc.getNumber());
            tn.setCountryCode(doc.getCountryCode());
            tn.setAreaCode(doc.getAreaCode());
            if (doc.getStatus() != null) {
                tn.setStatus(TelephoneNumber.Status.valueOf(doc.getStatus()));
            }
            tn.setAllocatedUserId(doc.getAllocatedUserId());
            tn.setReservedUntil(doc.getReservedUntil());
            rows.add(tn);
        }
        return new PageResponse<>(rows, page, size, hits.getTotalHits());
    }

    private TelephoneNumber getOrThrow(String number) {
        Optional<TelephoneNumber> opt = telDao.findByNumber(number);
        return opt.orElseThrow(() -> new ResourceNotFoundException(String.format(CommonConstants.NUMBER_NOT_FOUND, number)));
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

    private void syncToIndex(TelephoneNumber tn) {
        TelephoneDocument doc = new TelephoneDocument();
        doc.setNumber(tn.getNumber());
        doc.setCountryCode(tn.getCountryCode());
        doc.setAreaCode(tn.getAreaCode());
        if (tn.getStatus() != null) {
            doc.setStatus(tn.getStatus().name());
        }
        if (tn.getNumber() != null) {
            doc.setNumberDigits(tn.getNumber().replaceAll("\\D", ""));
        }
        doc.setAllocatedUserId(tn.getAllocatedUserId());
        doc.setReservedUntil(tn.getReservedUntil());
        esOps.save(doc);
    }

    @Transactional
    public TelephoneNumber reserve(String number, String userId, Duration hold) {
        TelephoneNumber tn = getOrThrow(number);
        if (tn.getStatus() != TelephoneNumber.Status.AVAILABLE) {
            throw new BusinessRuleViolationException(CommonConstants.ONLY_AVAILABLE_CAN_BE_RESERVED);
        }
        TelephoneNumber next = cloneState(tn);
        next.setStatus(TelephoneNumber.Status.RESERVED);
        next.setAllocatedUserId(userId);
        next.setReservedUntil(Instant.now().plus(hold));
        int updated = telDao.updateWithVersion(tn.getId(), tn.getVersion(), next);
        if (updated == 0) {
            throw new IllegalStateException(CommonConstants.CONCURRENT_UPDATE_DETECTED);
        }
        writeAudit(tn, next, userId, String.format(CommonConstants.RESERVED_FOR_TEMPLATE, hold.toMinutes()));
        syncToIndex(next);
        return getOrThrow(number);
    }

    @Transactional
    public TelephoneNumber allocate(String number, String userId) {
        TelephoneNumber tn = getOrThrow(number);
        if (tn.getReservedUntil() != null && tn.getReservedUntil().isBefore(Instant.now())) {
            throw new IllegalStateException(CommonConstants.RESERVATION_EXPIRED);
        }
        if (tn.getStatus() != TelephoneNumber.Status.RESERVED) {
            throw new BusinessRuleViolationException(CommonConstants.ONLY_RESERVED_CAN_BE_ALLOCATED);
        }
        if (tn.getAllocatedUserId() == null || !tn.getAllocatedUserId().equals(userId)) {
            throw new BusinessRuleViolationException(CommonConstants.NUMBER_RESERVED_FOR_DIFFERENT_USER);
        }
        TelephoneNumber next = cloneState(tn);
        next.setStatus(TelephoneNumber.Status.ALLOCATED);
        // keep allocatedUserId; clear reservedUntil
        next.setReservedUntil(null);
        int updated = telDao.updateWithVersion(tn.getId(), tn.getVersion(), next);
        if (updated == 0) {
            throw new IllegalStateException(CommonConstants.CONCURRENT_UPDATE_DETECTED);
        }
        writeAudit(tn, next, userId, CommonConstants.ALLOCATED);
        syncToIndex(next);
        return getOrThrow(number);
    }

    @Transactional
    public TelephoneNumber activate(String number, String userId) {
        TelephoneNumber tn = getOrThrow(number);
        if (tn.getStatus() != TelephoneNumber.Status.ALLOCATED) {
            throw new BusinessRuleViolationException(CommonConstants.ONLY_ALLOCATED_CAN_BE_ACTIVATED);
        }
        if (tn.getAllocatedUserId() == null || !tn.getAllocatedUserId().equals(userId)) {
            throw new BusinessRuleViolationException(CommonConstants.NUMBER_ALLOCATED_TO_DIFFERENT_USER);
        }
        TelephoneNumber next = cloneState(tn);
        next.setStatus(TelephoneNumber.Status.ACTIVATED);
        int updated = telDao.updateWithVersion(tn.getId(), tn.getVersion(), next);
        if (updated == 0) {
            throw new IllegalStateException(CommonConstants.CONCURRENT_UPDATE_DETECTED);
        }
        writeAudit(tn, next, userId, CommonConstants.ACTIVATED);
        syncToIndex(next);
        return getOrThrow(number);
    }

    @Transactional
    public TelephoneNumber deactivate(String number, String userId) {
        TelephoneNumber tn = getOrThrow(number);
        if (tn.getStatus() != TelephoneNumber.Status.ACTIVATED) {
            throw new BusinessRuleViolationException(CommonConstants.ONLY_ACTIVATED_CAN_BE_DEACTIVATED);
        }
        TelephoneNumber next = cloneState(tn);
        next.setStatus(TelephoneNumber.Status.DEACTIVATED);
        int updated = telDao.updateWithVersion(tn.getId(), tn.getVersion(), next);
        if (updated == 0) {
            throw new IllegalStateException(CommonConstants.CONCURRENT_UPDATE_DETECTED);
        }
        writeAudit(tn, next, userId, CommonConstants.DEACTIVATED);
        syncToIndex(next);
        return getOrThrow(number);
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
}
