package br.com.hulysses.business_partner_service.businesspartner.persistence;

import br.com.hulysses.business_partner_service.businesspartner.domain.BusinessPartnerActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BusinessPartnerActivityRepository extends JpaRepository<BusinessPartnerActivity, String> {
    List<BusinessPartnerActivity> findByBusinessPartnerIdOrderByOccurredAtAsc(Long businessPartnerId);
}
