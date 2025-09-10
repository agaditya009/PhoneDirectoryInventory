package com.assignment.phoneinventory.dto;

import java.time.LocalDateTime;
import java.util.List;

public class JobStatusResponse {
    public long executionId;
    public String jobId;
    public String jobName;
    public String status;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public String exitCode;
    public String exitDescription;
    public List<StepStatus> steps;

    public JobStatusResponse() {}
    public JobStatusResponse(long executionId, String jobId, String jobName, String status,
                             LocalDateTime startTime, LocalDateTime endTime,
                             String exitCode, String exitDescription,
                             List<StepStatus> steps) {
        this.executionId = executionId;
        this.jobId = jobId;
        this.jobName = jobName;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.exitCode = exitCode;
        this.exitDescription = exitDescription;
        this.steps = steps;
    }


}
