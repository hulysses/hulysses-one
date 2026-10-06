package br.com.hulysses.business_partner_service.businesspartner.application;

import br.com.hulysses.business_partner_service.businesspartner.presentation.dto.BusinessPartnerImportResponse;
import br.com.hulysses.business_partner_service.shared.exception.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class BusinessPartnerImportService {
    private static final Logger LOG = LoggerFactory.getLogger(BusinessPartnerImportService.class);
    private final JobOperator operator;
    private final Job job;
    private final String sample;

    public BusinessPartnerImportService(JobOperator operator, Job businessPartnerImportJob,
            @Value("${batch.business-partner.sample:classpath:batch/business-partners.csv}") String sample) {
        this.operator = operator;
        this.job = businessPartnerImportJob;
        this.sample = sample;
    }

    public BusinessPartnerImportResponse importCsv(MultipartFile file) throws Exception {
        Path temporary = null;
        try {
            String inputFile = sample;
            if (file != null) {
                if (file.isEmpty()) {
                    throw new DomainException("CSV file is empty");
                }
                temporary = Files.createTempFile("business-partners-", ".csv");
                file.transferTo(temporary);
                inputFile = temporary.toUri().toString();
            }
            var parameters = new JobParametersBuilder().addString("inputFile", inputFile)
                    .addString("importId", UUID.randomUUID().toString()).toJobParameters();
            var execution = operator.start(job, parameters);
            var response = BusinessPartnerImportResponse.from(execution);
            LOG.info("Business partner import finished: {}", response);
            return response;
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException exception) {
                    LOG.warn("Could not remove temporary CSV", exception);
                }
            }
        }
    }
}
