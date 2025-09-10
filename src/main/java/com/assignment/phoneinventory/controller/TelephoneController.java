package com.assignment.phoneinventory.controller;

import com.assignment.phoneinventory.domain.TelephoneNumber;
import com.assignment.phoneinventory.dto.JobStatusResponse;
import com.assignment.phoneinventory.dto.PageResponse;
import com.assignment.phoneinventory.mapper.JobStatusMapper;
import com.assignment.phoneinventory.service.BatchJobService;
import com.assignment.phoneinventory.service.TelephoneService;
import com.assignment.phoneinventory.constants.CommonConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.Duration;

@RestController
@RequestMapping(path = "/api/phones", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Telephone Inventory")
public class TelephoneController {

    private final TelephoneService service;
    private final JobLauncher jobLauncher;
    private final Job importJob;
    private final JobExplorer jobExplorer;
    private final JobStatusMapper jobStatusMapper;
    private final BatchJobService batchJobService;

    public TelephoneController(TelephoneService service, JobLauncher jobLauncher, Job importJob, JobExplorer jobExplorer, JobStatusMapper jobStatusMapper, BatchJobService batchJobService) {
        this.service = service;
        this.jobLauncher = jobLauncher;
        this.importJob = importJob;
        this.jobExplorer = jobExplorer;
        this.jobStatusMapper = jobStatusMapper;
        this.batchJobService = batchJobService;
    }

    @GetMapping
    @Operation(summary = "Search telephone numbers with pagination")
    public PageResponse<TelephoneNumber> search(
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String contains,
            @RequestParam(required = false) TelephoneNumber.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(countryCode, areaCode, contains, status, page, size);
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload CSV for batch import (Spring Batch)")
    public ResponseEntity<JobStatusResponse> uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        File tmp = File.createTempFile("upload-", "-" + file.getOriginalFilename());
        file.transferTo(tmp);

        String jobId = batchJobService.newJob(file.getOriginalFilename());
        JobParametersBuilder paramsBuilder = new JobParametersBuilder()
                .addString(CommonConstants.FILE_PATH, tmp.getAbsolutePath())
                .addString(CommonConstants.JOB_ID, jobId)
                .addLong("time", System.currentTimeMillis());
        JobExecution execution = jobLauncher.run(importJob, paramsBuilder.toJobParameters());
        return ResponseEntity.ok(jobStatusMapper.from(execution));
    }

    @PostMapping("/{number}/reserve")
    @Operation(summary = "Reserve a number for a user for a given hold duration (minutes)")
    public TelephoneNumber reserve(@PathVariable String number,
                                   @RequestParam String userId,
                                   @RequestParam(defaultValue = "30") int minutes) {
        return service.reserve(number, userId, Duration.ofMinutes(minutes));
    }

    @PostMapping("/{number}/allocate")
    @Operation(summary = "Allocate a reserved number to the user")
    public TelephoneNumber allocate(@PathVariable String number, @RequestParam String userId) {
        return service.allocate(number, userId);
    }

    @PostMapping("/{number}/activate")
    @Operation(summary = "Activate an allocated number")
    public TelephoneNumber activate(@PathVariable String number, @RequestParam String userId) {
        return service.activate(number, userId);
    }

    @PostMapping("/{number}/deactivate")
    @Operation(summary = "Deactivate a number")
    public TelephoneNumber deactivate(@PathVariable String number, @RequestParam String userId) {
        return service.deactivate(number, userId);
    }

    @GetMapping("/jobs/{executionId}")
    @Operation(summary = "Get Spring Batch job execution status by executionId")
    public ResponseEntity<JobStatusResponse> jobStatus(@PathVariable long executionId) {
        JobExecution exec = jobExplorer.getJobExecution(executionId);
        if (exec == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(jobStatusMapper.from(exec));
    }

    @GetMapping("/jobs/track/{jobId}")
    @Operation(summary = "Get custom job status by jobId (batch_jobs table)")
    public ResponseEntity<?> jobStatusByJobId(@PathVariable String jobId) {
        return batchJobService.get(jobId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
