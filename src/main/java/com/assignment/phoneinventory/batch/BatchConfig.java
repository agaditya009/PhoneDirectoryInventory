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

import java.util.regex.Pattern;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Bean
    public ItemProcessor<PhoneCsv, PhoneCsv> processor() {
        return item -> {
            if (item == null) return null;
            String n = item.getNumber();
            // one-or-more non-digits -> remove; Java string needs \\D+
            String digits = (n == null) ? "" : n.replaceAll("\\\\D+", "");
            item.setNumberDigits(digits);
            return item;
        };
    }


    @Bean
    public JdbcBatchItemWriter<PhoneCsv> writer(DataSource dataSource) {
        JdbcBatchItemWriter<PhoneCsv> writer = new JdbcBatchItemWriter<>();
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        // H2 MERGE for upsert on unique key(number)
        writer.setSql(
                "MERGE INTO telephone_numbers (number, country_code, area_code, prefix, status, version) " +
                        "KEY(number) VALUES (:number, :countryCode, :areaCode, :prefix, 'AVAILABLE', 0)"
        );
        writer.setDataSource(dataSource);
        return writer;
    }

    @Bean
    public Step importStep(StepBuilderFactory stepBuilderFactory,
                           FlatFileItemReader<PhoneCsv> reader,
                           ItemProcessor<PhoneCsv, PhoneCsv> processor,
                           JdbcBatchItemWriter<PhoneCsv> writer) {
        return stepBuilderFactory.get("importStep")
                .<PhoneCsv, PhoneCsv>chunk(100)
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
        tokenizer.setNames("number","countryCode","areaCode","prefix");

        // 3) Be tolerant of anomalies (blank lines / trailing commas)
        tokenizer.setStrict(false);

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
