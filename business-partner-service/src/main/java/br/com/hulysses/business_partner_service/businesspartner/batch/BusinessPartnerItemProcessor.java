package br.com.hulysses.business_partner_service.businesspartner.batch;

import br.com.hulysses.business_partner_service.businesspartner.domain.BusinessPartnerRole;
import br.com.hulysses.business_partner_service.businesspartner.domain.BusinessPartnerType;
import br.com.hulysses.business_partner_service.businesspartner.persistence.BusinessPartnerRepository;
import br.com.hulysses.business_partner_service.businesspartner.presentation.dto.BusinessPartnerRequest;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@StepScope
public class BusinessPartnerItemProcessor implements ItemProcessor<BusinessPartnerCsvRow, BusinessPartnerRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(BusinessPartnerItemProcessor.class);
    private final Validator validator;
    private final BusinessPartnerRepository repository;
    // Step scope prevents one import from sharing this set with another import.
    private final Set<String> documents = new HashSet<>();

    public BusinessPartnerItemProcessor(Validator validator, BusinessPartnerRepository repository) {
        this.validator = validator;
        this.repository = repository;
    }

    @Override
    public BusinessPartnerRequest process(BusinessPartnerCsvRow row) {
        BusinessPartnerRequest request;
        try {
            request = new BusinessPartnerRequest(row.name().trim(),
                    row.document().trim().replaceAll("[.\\-/\\s]", ""),
                    row.email().trim().toLowerCase(Locale.ROOT), row.phone().trim(),
                    BusinessPartnerType.valueOf(row.type().trim().toUpperCase(Locale.ROOT)),
                    Arrays.stream(row.roles().split(";", -1))
                            .map(role -> BusinessPartnerRole.valueOf(role.trim().toUpperCase(Locale.ROOT)))
                            .collect(Collectors.toSet()), null, null);
        } catch (IllegalArgumentException exception) {
            LOG.warn("Filtered CSV row: invalid type or roles");
            return null;
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            LOG.warn("Filtered CSV row: validation failed for fields {}", violations.stream()
                    .map(violation -> violation.getPropertyPath().toString()).sorted().toList());
            return null;
        }
        if (!documents.add(request.document()) || repository.existsByDocument(request.document())) {
            LOG.info("Filtered CSV row: document already imported or registered");
            return null;
        }
        return request;
    }
}
