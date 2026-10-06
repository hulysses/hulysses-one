package br.com.hulysses.business_partner_service.businesspartner.batch;

import br.com.hulysses.business_partner_service.businesspartner.presentation.dto.BusinessPartnerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.batch.autoconfigure.BatchTaskExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class BusinessPartnerBatchConfig {
    private static final Logger LOG = LoggerFactory.getLogger(BusinessPartnerBatchConfig.class);
    public static final String CSV_HEADER = "name,document,email,phone,type,roles";

    @Bean
    @BatchTaskExecutor
    SyncTaskExecutor batchTaskExecutor() {
        // Demonstration endpoint waits for the final counters; no background scheduler or upload race.
        return new SyncTaskExecutor();
    }

    @Bean
    @StepScope
    FlatFileItemReader<BusinessPartnerCsvRow> businessPartnerCsvReader(
            @Value("#{jobParameters['inputFile']}") String inputFile, ResourceLoader resources) {
        return new FlatFileItemReaderBuilder<BusinessPartnerCsvRow>()
                .name("businessPartnerCsvReader").resource(resources.getResource(inputFile))
                .encoding("UTF-8").linesToSkip(1)
                .skippedLinesCallback(header -> {
                    if (!CSV_HEADER.equals(header.replace("\uFEFF", "").trim())) {
                        throw new IllegalArgumentException("Expected CSV header: " + CSV_HEADER);
                    }
                })
                .delimited().names("name", "document", "email", "phone", "type", "roles")
                .fieldSetMapper(fields -> new BusinessPartnerCsvRow(fields.readString("name"),
                        fields.readString("document"), fields.readString("email"), fields.readString("phone"),
                        fields.readString("type"), fields.readString("roles"))).build();
    }

    @Bean
    Step businessPartnerImportStep(JobRepository repository, PlatformTransactionManager transactionManager,
            FlatFileItemReader<BusinessPartnerCsvRow> businessPartnerCsvReader,
            BusinessPartnerItemProcessor processor, BusinessPartnerItemWriter writer) {
        return new StepBuilder("businessPartnerImportStep", repository)
                .<BusinessPartnerCsvRow, BusinessPartnerRequest>chunk(10)
                .transactionManager(transactionManager).reader(businessPartnerCsvReader)
                .processor(processor).writer(writer)
                .listener(new ChunkListener<BusinessPartnerCsvRow, BusinessPartnerRequest>() {
                    @Override
                    public void afterChunk(Chunk<BusinessPartnerRequest> chunk) {
                        LOG.info("Business partner import chunk processed: written={} chunkSize=10", chunk.size());
                    }
                }).build();
    }

    @Bean
    Job businessPartnerImportJob(JobRepository repository, Step businessPartnerImportStep) {
        return new JobBuilder("businessPartnerImportJob", repository).start(businessPartnerImportStep).build();
    }
}
