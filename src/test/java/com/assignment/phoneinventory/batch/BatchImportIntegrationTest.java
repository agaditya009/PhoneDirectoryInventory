package com.assignment.phoneinventory.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBatchTest
@SpringBootTest
@Testcontainers
class BatchImportIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8")
            .withDatabaseName("phones")
            .withUsername("app")
            .withPassword("app");

    @DynamicPropertySource
    static void mysqlProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    @Autowired
    private Job importJob;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private JobLauncherTestUtils jobLauncherTestUtils;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils = new JobLauncherTestUtils();
        jobLauncherTestUtils.setJobLauncher(jobLauncher);
        jobLauncherTestUtils.setJob(importJob);
    }

    private Path copyResource(String resourcePath) throws Exception {
        Path dest = tempDir.resolve(Path.of(resourcePath).getFileName());
        Files.copy(new ClassPathResource(resourcePath).getInputStream(), dest);
        return dest;
    }

    @Test
    void csvImportHandlesEdgeCasesAndUpserts() throws Exception {
        // initial import with header, blank line and duplicate rows
        Path file1 = copyResource("batch/phones_initial.csv");
        JobParameters params1 = new JobParametersBuilder()
                .addString("file.path", file1.toString())
                .toJobParameters();
        JobExecution exec1 = jobLauncherTestUtils.launchJob(params1);
        assertThat(exec1.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM telephone_numbers", Integer.class);
        assertThat(count).isEqualTo(2); // duplicate row ignored

        // second import with one row changed
        Path file2 = copyResource("batch/phones_update.csv");
        JobParameters params2 = new JobParametersBuilder()
                .addString("file.path", file2.toString())
                .addLong("run.id", 1L) // ensure unique job instance
                .toJobParameters();
        JobExecution exec2 = jobLauncherTestUtils.launchJob(params2);
        assertThat(exec2.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

        // verify first number unchanged, second updated
        String area1 = jdbcTemplate.queryForObject(
                "SELECT area_code FROM telephone_numbers WHERE number=?",
                String.class,
                "+91-1111111111");
        String area2 = jdbcTemplate.queryForObject(
                "SELECT area_code FROM telephone_numbers WHERE number=?",
                String.class,
                "+91-2222222222");

        assertThat(area1).isEqualTo("080"); // no-op update skipped
        assertThat(area2).isEqualTo("081"); // updated
    }
}

