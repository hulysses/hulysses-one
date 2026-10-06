package br.com.hulysses.business_partner_service.businesspartner.application;

import br.com.hulysses.business_partner_service.businesspartner.domain.BusinessPartnerActivity;
import br.com.hulysses.business_partner_service.businesspartner.messaging.BusinessPartnerCreatedEvent;
import br.com.hulysses.business_partner_service.businesspartner.persistence.BusinessPartnerActivityRepository;
import br.com.hulysses.business_partner_service.businesspartner.presentation.dto.BusinessPartnerActivityResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BusinessPartnerActivityService {
    private final BusinessPartnerActivityRepository repository;

    public BusinessPartnerActivityService(BusinessPartnerActivityRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(BusinessPartnerCreatedEvent event) {
        if (!repository.existsById(event.eventId())) {
            repository.save(new BusinessPartnerActivity(event.eventId(), event.businessPartnerId(),
                    event.eventType(), event.occurredAt()));
        }
    }

    public List<BusinessPartnerActivityResponse> findByPartner(Long id) {
        return repository.findByBusinessPartnerIdOrderByOccurredAtAsc(id).stream()
                .map(BusinessPartnerActivityResponse::from).toList();
    }
}
