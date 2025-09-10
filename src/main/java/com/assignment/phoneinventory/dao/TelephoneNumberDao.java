package com.assignment.phoneinventory.dao;

import com.assignment.phoneinventory.domain.TelephoneNumber;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.assignment.phoneinventory.constants.TelephoneCols.*;
import static com.assignment.phoneinventory.constants.TelephoneConstants.*;


@Repository
public class TelephoneNumberDao {

    private final JdbcTemplate jdbc;

    public TelephoneNumberDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<TelephoneNumber> ROW_MAPPER = new RowMapper<TelephoneNumber>() {
        @Override
        public TelephoneNumber mapRow(ResultSet rs, int rowNum) throws SQLException {
            TelephoneNumber t = new TelephoneNumber();
            t.setId(rs.getLong(ID));
            t.setNumber(rs.getString(NUMBER));
            t.setCountryCode(rs.getString(COUNTRY_CODE));
            t.setAreaCode(rs.getString(AREA_CODE));
            String st = rs.getString(STATUS);
            t.setStatus(st == null ? null : TelephoneNumber.Status.valueOf(st));
            t.setAllocatedUserId(rs.getString(ALLOCATED_USER_ID));
            Timestamp ts = rs.getTimestamp(RESERVED_UNTIL);
            t.setReservedUntil(ts == null ? null : ts.toInstant());
            t.setVersion(rs.getLong(VERSION));
            return t;
        }
    };

    public Optional<TelephoneNumber> findById(Long id) {
        List<TelephoneNumber> list = jdbc.query(SELECT_BY_ID, ROW_MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<TelephoneNumber> findByNumber(String number) {
        List<TelephoneNumber> list = jdbc.query(SELECT_BY_NUMBER, ROW_MAPPER, number);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<TelephoneNumber> findAll() {
        return jdbc.query(SELECT_BASE, ROW_MAPPER);
    }

    
    public List<TelephoneNumber> search(String cc, String ac, String contains, String status, int page, int size) {
        StringBuilder sql = new StringBuilder(SELECT_BASE); // "... FROM telephone_numbers WHERE 1=1"
        List<Object> args = new ArrayList<>();

        if (cc != null && !cc.isEmpty()) { sql.append(" AND country_code = ?"); args.add(cc); }
        if (ac != null && !ac.isEmpty()) { sql.append(" AND area_code = ?"); args.add(ac); }

        if (contains != null && !contains.isEmpty()) {
            sql.append(" AND number LIKE ?");
            args.add("%" + contains + "%");
        }

        if (status != null && !status.isEmpty()) { sql.append(" AND status = ?"); args.add(status); }

        sql.append(SEARCH_ORDER_LIMIT_OFFSET); // " ORDER BY id ASC LIMIT ? OFFSET ?"
        args.add(size);
        args.add(page * size);

        return jdbc.query(sql.toString(), ROW_MAPPER, args.toArray());
    }

    public long count(String cc, String ac, String contains, String status) {
        StringBuilder sql = new StringBuilder(COUNT_BASE); // "SELECT COUNT(*) FROM telephone_numbers WHERE 1=1"
        List<Object> args = new ArrayList<>();

        if (cc != null && !cc.isEmpty()) { sql.append(" AND country_code = ?"); args.add(cc); }
        if (ac != null && !ac.isEmpty()) { sql.append(" AND area_code = ?"); args.add(ac); }

        if (contains != null && !contains.isEmpty()) {
            sql.append(" AND number LIKE ?");
            args.add("%" + contains + "%");
        }

        if (status != null && !status.isEmpty()) { sql.append(" AND status = ?"); args.add(status); }

        return jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
    }

    public int updateWithVersion(Long id, long expectedVersion, TelephoneNumber newState) {
        Timestamp reserved = newState.getReservedUntil() == null ? null : Timestamp.from(newState.getReservedUntil());
        return jdbc.update(
                UPDATE_WITH_VERSION,
                newState.getStatus().name(),
                newState.getAllocatedUserId(),
                reserved,
                id,
                expectedVersion
        );
    }

    public boolean existsByNumber(String number) {
        Integer cnt = jdbc.queryForObject(EXISTS_BY_NUMBER, Integer.class, number);
        return cnt > 0;
    }

    public int insert(TelephoneNumber t) {
        return jdbc.update(
                INSERT_ROW,
                t.getNumber(),
                t.getCountryCode(),
                t.getAreaCode(),
                t.getStatus().name(),
                t.getAllocatedUserId(),
                t.getReservedUntil() == null ? null : Timestamp.from(t.getReservedUntil()),
                t.getVersion()
        );
    }

    /**
     * Simple "upsert" by guarding with exists() first.
     * If you prefer DB-native upsert, keep your MERGE/ON CONFLICT in the batch writer.
     */
    public int upsertNumber(String number, String cc, String ac) {
        if (existsByNumber(number)) return 0;
        TelephoneNumber t = new TelephoneNumber();
        t.setNumber(number);
        t.setCountryCode(cc);
        t.setAreaCode(ac);
        t.setStatus(TelephoneNumber.Status.AVAILABLE);
        t.setVersion(0);
        return insert(t);
    }
}
