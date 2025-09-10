package com.assignment.phoneinventory.dao;

import com.assignment.phoneinventory.constants.AuditLogConstants;
import com.assignment.phoneinventory.domain.AuditLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class AuditLogDao {
    private final JdbcTemplate jdbc;

    public AuditLogDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(AuditLog log) {
        jdbc.update(AuditLogConstants.INSERT_LOG,
                log.getNumberId(), log.getFromState(), log.getToState(), log.getUserId(),
                Timestamp.from(log.getTimestamp() == null ? Instant.now() : log.getTimestamp()),
                log.getNote());
    }
}
