package com.assignment.phoneinventory.batch;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import com.assignment.phoneinventory.exception.InvalidCsvFormatException;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Bean
    public ItemProcessor<PhoneCsv, PhoneCsv> processor() {
        return item -> {
            if (item == null) return null;

            if (item.getNumber() == null || item.getCountryCode() == null || item.getAreaCode() == null) {
                throw new InvalidCsvFormatException("Missing required column");
            }

            String digits = item.getNumber().replaceAll("\\D+", "");
            item.setNumberDigits(digits);

            return item;
        };
    }


    @Bean
    public JdbcBatchItemWriter<PhoneCsv> writer(DataSource dataSource) {
        JdbcBatchItemWriter<PhoneCsv> writer = new JdbcBatchItemWriter<>();
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        writer.setAssertUpdates(false); // allow no-op when row unchanged
        // H2-specific upsert using MERGE with change detection to skip no-op updates
        writer.setSql(
                "MERGE INTO telephone_numbers t " +
                        "USING (VALUES (:number, :countryCode, :areaCode, :numberDigits)) s(number, country_code, area_code, number_digits) " +
                        "ON t.number = s.number " +
                        "WHEN MATCHED THEN UPDATE SET " +
                        "country_code = s.country_code, " +
                        "area_code = s.area_code, " +
                        "status = 'AVAILABLE', " +
                        "version = 0, " +
                        "number_digits = s.number_digits " +
                        "WHERE t.country_code IS DISTINCT FROM s.country_code " +
                        "OR t.area_code IS DISTINCT FROM s.area_code " +
                        "OR t.status <> 'AVAILABLE' " +
                        "OR t.version <> 0 " +
                        "OR t.number_digits IS DISTINCT FROM s.number_digits " +
                        "WHEN NOT MATCHED THEN INSERT (number, country_code, area_code, status, version, number_digits) " +
                        "VALUES (s.number, s.country_code, s.area_code, 'AVAILABLE', 0, s.number_digits)"
        );
        writer.setDataSource(dataSource);
        return writer;
    }

    @Bean
    public Step importStep(StepBuilderFactory stepBuilderFactory,
                           FlatFileItemReader<PhoneCsv> reader,
                           ItemProcessor<PhoneCsv, PhoneCsv> processor,
                           JdbcBatchItemWriter<PhoneCsv> writer,
                           @Value("${batch.chunk.size:1000}") int chunkSize) {
        return stepBuilderFactory.get("importStep")
                .<PhoneCsv, PhoneCsv>chunk(chunkSize)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job importJob(JobBuilderFactory jobBuilderFactory, Step importStep) {
        return jobBuilderFactory.get("importJob")
                .incrementer(new RunIdIncrementer())
                .flow(importStep)
                .end()
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<PhoneCsv> reader(@Value("#{jobParameters['file.path']}") String filePath) {
        FlatFileItemReader<PhoneCsv> reader = new FlatFileItemReader<>();
        reader.setName("phoneCsvReader");
        reader.setResource(new FileSystemResource(filePath));

        // 1) Skip header row if present
        reader.setLinesToSkip(1);
        reader.setSkippedLinesCallback(line -> { /* no-op */ });

        // 2) Ignore blank/empty lines safely
        reader.setRecordSeparatorPolicy(new org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy());

        DefaultLineMapper<PhoneCsv> lineMapper = new DefaultLineMapper<>();

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setNames("number","countryCode","areaCode");

        // 3) Fail if columns are missing
        tokenizer.setStrict(true);

        lineMapper.setLineTokenizer(tokenizer);

        BeanWrapperFieldSetMapper<PhoneCsv> fsm = new BeanWrapperFieldSetMapper<>();
        fsm.setTargetType(PhoneCsv.class);
        lineMapper.setFieldSetMapper(fsm);

        reader.setLineMapper(lineMapper);

        // File must exist, but row-level leniency is handled above
        reader.setStrict(true);
        return reader;
    }

}
