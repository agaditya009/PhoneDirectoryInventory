package com.assignment.phoneinventory.dao;

import com.assignment.phoneinventory.domain.BatchJob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static com.assignment.phoneinventory.constants.BatchJobConstants.*;

@Repository
public class BatchJobDao {

    private final JdbcTemplate jdbc;

    public BatchJobDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertQueued(String jobId, String fileName) {
        jdbc.update(INSERT_QUEUED, jobId, fileName);
    }

    public void markRunning(String jobId) {
        jdbc.update(MARK_RUNNING, jobId);
    }

    public void heartbeat(String jobId, int processed, int failed) {
        jdbc.update(HEARTBEAT, processed, failed, jobId);
    }

    public void complete(String jobId) {
        jdbc.update(COMPLETE, jobId);
    }

    public void fail(String jobId, String error) {
        jdbc.update(FAIL, error, jobId);
    }

    public Optional<BatchJob> find(String jobId) {
        var list = jdbc.query(FIND_BY_JOB_ID, (rs, i) -> map(rs), jobId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    private BatchJob map(ResultSet rs) throws SQLException {
        BatchJob j = new BatchJob();
        j.setId(rs.getLong("id"));
        j.setJobId(rs.getString("job_id"));
        j.setFileName(rs.getString("file_name"));
        j.setStatus(rs.getString("status"));
        var s = rs.getTimestamp("started_at");
        if (s != null) {
            j.setStartedAt(s.toInstant());
        }
        var e = rs.getTimestamp("ended_at");
        if (e != null) {
            j.setEndedAt(e.toInstant());
        }
        j.setProcessedRecords(rs.getInt("processed_records"));
        j.setFailedRecords(rs.getInt("failed_records"));
        j.setErrorMessage(rs.getString("error_message"));
        var c = rs.getTimestamp("created_at");
        if (c != null) {
            j.setCreatedAt(c.toInstant());
        }
        var u = rs.getTimestamp("updated_at");
        if (u != null) {
            j.setUpdatedAt(u.toInstant());
        }
        return j;
    }
}

