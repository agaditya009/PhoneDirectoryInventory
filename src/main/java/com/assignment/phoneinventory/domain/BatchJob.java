package com.assignment.phoneinventory.domain;

import java.time.Instant;

public class BatchJob {

    private Long id;
    private String jobId;
    private String fileName;
    private String status;
    private Instant startedAt;
    private Instant endedAt;
    private Integer processedRecords;
    private Integer failedRecords;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String v) {
        jobId = v;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String v) {
        fileName = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant v) {
        startedAt = v;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant v) {
        endedAt = v;
    }

    public Integer getProcessedRecords() {
        return processedRecords;
    }

    public void setProcessedRecords(Integer v) {
        processedRecords = v;
    }

    public Integer getFailedRecords() {
        return failedRecords;
    }

    public void setFailedRecords(Integer v) {
        failedRecords = v;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String v) {
        errorMessage = v;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant v) {
        createdAt = v;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant v) {
        updatedAt = v;
    }
}
