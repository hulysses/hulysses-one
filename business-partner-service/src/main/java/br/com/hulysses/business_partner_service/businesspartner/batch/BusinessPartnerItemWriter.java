package br.com.hulysses.business_partner_service.businesspartner.batch;

import br.com.hulysses.business_partner_service.businesspartner.application.BusinessPartnerService;
import br.com.hulysses.business_partner_service.businesspartner.presentation.dto.BusinessPartnerRequest;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class BusinessPartnerItemWriter implements ItemWriter<BusinessPartnerRequest> {
    private final BusinessPartnerService service;

    public BusinessPartnerItemWriter(BusinessPartnerService service) {
        this.service = service;
    }

    @Override
    public void write(Chunk<? extends BusinessPartnerRequest> chunk) {
        // All calls join the Step's transaction; validation and persistence remain in the existing service.
        for (BusinessPartnerRequest request : chunk) {
            service.create(request);
        }
    }
}
