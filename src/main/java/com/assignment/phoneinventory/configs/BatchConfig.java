package com.assignment.phoneinventory.configs;

import javax.sql.DataSource;

import com.assignment.phoneinventory.batch.ElasticsearchItemWriter;
import com.assignment.phoneinventory.batch.JobAuditListener;
import com.assignment.phoneinventory.batch.PhoneCsv;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.util.Arrays;

import com.assignment.phoneinventory.exception.InvalidCsvFormatException;
import com.assignment.phoneinventory.constants.CommonConstants;

import static com.assignment.phoneinventory.constants.BatchJobConstants.UPSERT_TELEPHONE_NUMBER;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Bean
    public ItemProcessor<PhoneCsv, PhoneCsv> processor() {
        return item -> {

            if (item.getNumber() == null || item.getCountryCode() == null || item.getAreaCode() == null) {
                throw new InvalidCsvFormatException(CommonConstants.MISSING_REQUIRED_COLUMN);
            }

            return item;
        };
    }


    @Bean
    public JdbcBatchItemWriter<PhoneCsv> jdbcWriter(DataSource dataSource) throws Exception {
        JdbcBatchItemWriter<PhoneCsv> writer = new JdbcBatchItemWriter<>();
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        writer.setAssertUpdates(false); // allow no-op when row unchanged
        // MySQL upsert using INSERT ... ON DUPLICATE KEY UPDATE with conditional assignments
        writer.setSql(UPSERT_TELEPHONE_NUMBER);
        writer.setDataSource(dataSource);
        return writer;
    }

    @Bean
    public ElasticsearchItemWriter elasticsearchWriter(ElasticsearchOperations operations) {
        return new ElasticsearchItemWriter(operations);
    }

    @Bean
    public CompositeItemWriter<PhoneCsv> compositeWriter(JdbcBatchItemWriter<PhoneCsv> jdbcWriter,
                                                         ElasticsearchItemWriter elasticsearchWriter) {
        CompositeItemWriter<PhoneCsv> writer = new CompositeItemWriter<>();
        writer.setDelegates(Arrays.asList(jdbcWriter, elasticsearchWriter));
        return writer;
    }

    @Bean
    public Step importStep(StepBuilderFactory stepBuilderFactory,
                           FlatFileItemReader<PhoneCsv> reader,
                           ItemProcessor<PhoneCsv, PhoneCsv> processor,
                           CompositeItemWriter<PhoneCsv> compositeWriter,
                           JobAuditListener jobAuditListener,
                           @Value("${batch.chunk.size:1000}") int chunkSize) {
        return stepBuilderFactory.get("importStep")
                .<PhoneCsv, PhoneCsv>chunk(chunkSize)
                .reader(reader)
                .processor(processor)
                .writer(compositeWriter)
                .listener((StepExecutionListener) jobAuditListener)
                .listener((ChunkListener) jobAuditListener)
                .build();
    }

    @Bean
    public Job importJob(JobBuilderFactory jobBuilderFactory, Step importStep, JobAuditListener jobAuditListener) {
        return jobBuilderFactory.get("importJob")
                .incrementer(new RunIdIncrementer())
                .listener((JobExecutionListener) jobAuditListener)
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
