package com.assignment.phoneinventory.mapper;

import com.assignment.phoneinventory.constants.CommonConstants;
import com.assignment.phoneinventory.dto.JobStatusResponse;
import com.assignment.phoneinventory.dto.StepStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
public class JobStatusMapper {

    public JobStatusResponse from(JobExecution exec) {
        LocalDateTime st = exec.getStartTime() == null ? null
                : LocalDateTime.ofInstant(exec.getStartTime().toInstant(), ZoneId.systemDefault());
        LocalDateTime et = exec.getEndTime() == null ? null
                : LocalDateTime.ofInstant(exec.getEndTime().toInstant(), ZoneId.systemDefault());

        List<StepStatus> steps = new ArrayList<>();
        for (StepExecution se : exec.getStepExecutions()) {
            steps.add(new StepStatus(
                    se.getStepName(),
                    se.getStatus().toString(),
                    se.getReadCount(),
                    se.getWriteCount(),
                    se.getReadSkipCount(),
                    se.getWriteSkipCount(),
                    se.getProcessSkipCount()
            ));
        }

        String jobId = exec.getJobParameters().getString(CommonConstants.JOB_ID);

        return new JobStatusResponse(
                exec.getId(),
                jobId,
                exec.getJobInstance().getJobName(),
                exec.getStatus().toString(),
                st,
                et,
                exec.getExitStatus().getExitCode(),
                exec.getExitStatus().getExitDescription(),
                steps
        );
    }
}
