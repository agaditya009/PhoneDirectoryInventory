package com.assignment.phoneinventory.dao;
import com.assignment.phoneinventory.domain.BatchJob;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet; import java.sql.SQLException; import java.util.Optional;
@Repository
public class BatchJobDao {
    private final JdbcTemplate jdbc;
    public BatchJobDao(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public void insertQueued(String jobId, String fileName){
        jdbc.update("INSERT INTO batch_jobs(job_id,file_name,status,created_at,updated_at,processed_records,failed_records) VALUES (?,?, 'QUEUED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)", jobId, fileName);
    }
    public void markRunning(String jobId){ jdbc.update("UPDATE batch_jobs SET status='RUNNING', started_at=COALESCE(started_at, CURRENT_TIMESTAMP), updated_at=CURRENT_TIMESTAMP WHERE job_id=?", jobId); }
    public void heartbeat(String jobId,int processed,int failed){ jdbc.update("UPDATE batch_jobs SET processed_records=processed_records+?, failed_records=failed_records+?, updated_at=CURRENT_TIMESTAMP WHERE job_id=?", processed, failed, jobId); }
    public void complete(String jobId){ jdbc.update("UPDATE batch_jobs SET status='COMPLETED', ended_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP WHERE job_id=?", jobId); }
    public void fail(String jobId, String error){ jdbc.update("UPDATE batch_jobs SET status='FAILED', ended_at=CURRENT_TIMESTAMP, error_message=?, updated_at=CURRENT_TIMESTAMP WHERE job_id=?", error, jobId); }
    public Optional<BatchJob> find(String jobId){
        var list = jdbc.query("SELECT * FROM batch_jobs WHERE job_id=?", (rs,i)->map(rs), jobId);
        return list.isEmpty()?Optional.empty():Optional.of(list.get(0));
    }
    private BatchJob map(ResultSet rs) throws SQLException {
        BatchJob j = new BatchJob();
        j.setId(rs.getLong("id")); j.setJobId(rs.getString("job_id")); j.setFileName(rs.getString("file_name")); j.setStatus(rs.getString("status"));
        var s = rs.getTimestamp("started_at"); if (s!=null) j.setStartedAt(s.toInstant());
        var e = rs.getTimestamp("ended_at"); if (e!=null) j.setEndedAt(e.toInstant());
        j.setProcessedRecords(rs.getInt("processed_records")); j.setFailedRecords(rs.getInt("failed_records"));
        j.setErrorMessage(rs.getString("error_message"));
        var c = rs.getTimestamp("created_at"); if (c!=null) j.setCreatedAt(c.toInstant());
        var u = rs.getTimestamp("updated_at"); if (u!=null) j.setUpdatedAt(u.toInstant());
        return j;
    }
}