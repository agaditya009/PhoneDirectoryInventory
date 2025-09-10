package com.assignment.phoneinventory.batch;

import com.assignment.phoneinventory.service.BatchJobService;
import com.assignment.phoneinventory.constants.CommonConstants;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobAuditListener implements JobExecutionListener, StepExecutionListener, ChunkListener {

    private final BatchJobService jobs;

    // Track last reported totals per job to send deltas in heartbeat
    private final Map<String, Integer> lastReadByJob  = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastFailByJob  = new ConcurrentHashMap<>();

    public JobAuditListener(BatchJobService jobs) {
        this.jobs = jobs;
    }

    // --- JobExecutionListener ---

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobId = jobExecution.getJobParameters().getString(CommonConstants.JOB_ID);
        if (jobId != null && !jobId.isBlank()) {
            jobs.markRunning(jobId);
            lastReadByJob.put(jobId, 0);
            lastFailByJob.put(jobId, 0);
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobId = jobExecution.getJobParameters().getString(CommonConstants.JOB_ID);

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            if (jobId != null && !jobId.isBlank()) {
                jobs.complete(jobId);
            }
        } else {
            if (jobId != null && !jobId.isBlank()) {
                jobs.fail(jobId, jobExecution.getAllFailureExceptions().toString());
            }
        }

        // cleanup to avoid leaks across runs
        if (jobId != null && !jobId.isBlank()) {
            lastReadByJob.remove(jobId);
            lastFailByJob.remove(jobId);
        }
    }

    // --- StepExecutionListener ---

    @Override
    public void beforeStep(StepExecution stepExecution) {
        // no-op
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return stepExecution.getExitStatus();
    }

    // --- ChunkListener ---

    @Override
    public void beforeChunk(ChunkContext context) {
        // no-op
    }

    @Override
    public void afterChunk(ChunkContext context) {
        String jobId = (String) context.getStepContext().getJobParameters().get(CommonConstants.JOB_ID);
        if (jobId == null || jobId.isBlank()) return;

        StepExecution se = context.getStepContext().getStepExecution();

        // Totals reported by Spring Batch so far
        int totalRead = se.getReadCount();
        int totalFail = se.getProcessSkipCount() + se.getWriteSkipCount();

        // Compute deltas since last heartbeat for this job
        int prevRead = lastReadByJob.getOrDefault(jobId, 0);
        int prevFail = lastFailByJob.getOrDefault(jobId, 0);

        int deltaRead = Math.max(0, totalRead - prevRead);
        int deltaFail = Math.max(0, totalFail - prevFail);

        // Only send heartbeat if there’s progress
        if (deltaRead > 0 || deltaFail > 0) {
            jobs.heartbeat(jobId, deltaRead, deltaFail);
            lastReadByJob.put(jobId, totalRead);
            lastFailByJob.put(jobId, totalFail);
        }
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        // You could also send a heartbeat or log here if desired
    }
}
