package com.assignment.phoneinventory.service;
import com.assignment.phoneinventory.dao.BatchJobDao;
import com.assignment.phoneinventory.domain.BatchJob;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter; import java.util.Optional; import java.util.UUID;
@Service
public class BatchJobService {
    private final BatchJobDao dao;
    public BatchJobService(BatchJobDao dao){this.dao=dao;}
    public String newJob(String fileName){
        String jobId = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(java.time.LocalDateTime.now()) + "-" + UUID.randomUUID().toString().substring(0,8);
        dao.insertQueued(jobId, fileName); return jobId;
    }
    public void markRunning(String jobId){ dao.markRunning(jobId); }
    public void heartbeat(String jobId, int processed, int failed){ dao.heartbeat(jobId, processed, failed); }
    public void complete(String jobId){ dao.complete(jobId); }
    public void fail(String jobId, String err){ dao.fail(jobId, err); }
    public Optional<BatchJob> get(String jobId){ return dao.find(jobId); }
}