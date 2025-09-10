package com.assignment.phoneinventory.controller;

import com.assignment.phoneinventory.dto.JobStatusResponse;
import com.assignment.phoneinventory.mapper.JobStatusMapper;
import com.assignment.phoneinventory.service.BatchJobService;
import com.assignment.phoneinventory.service.TelephoneService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TelephoneController.class)
class TelephoneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TelephoneService telephoneService;

    @MockBean
    private JobLauncher jobLauncher;

    @MockBean
    private Job importJob;

    @MockBean
    private JobExplorer jobExplorer;

    @MockBean
    private JobStatusMapper jobStatusMapper;

    @MockBean
    private BatchJobService batchJobService;

    @Test
    void uploadReturnsJobId() throws Exception {
        String jobId = "job-123";
        when(batchJobService.newJob(ArgumentMatchers.anyString())).thenReturn(jobId);

        JobExecution exec = new JobExecution(1L);
        when(jobLauncher.run(ArgumentMatchers.eq(importJob), ArgumentMatchers.any(JobParameters.class))).thenReturn(exec);

        JobStatusResponse response = new JobStatusResponse(
                exec.getId(), jobId, "importJob", "STARTING",
                LocalDateTime.now(), null,
                "", "", Collections.emptyList());
        when(jobStatusMapper.from(exec)).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file", "phones.csv", MediaType.TEXT_PLAIN_VALUE,
                "number,countryCode,areaCode\n123,1,2".getBytes());

        mockMvc.perform(multipart("/api/phones/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.executionId").value(1));
    }
}
