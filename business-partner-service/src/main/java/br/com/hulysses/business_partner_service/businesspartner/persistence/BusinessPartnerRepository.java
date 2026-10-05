package br.com.hulysses.business_partner_service.businesspartner.persistence;

import br.com.hulysses.business_partner_service.businesspartner.domain.BusinessPartner;
import br.com.hulysses.business_partner_service.businesspartner.domain.BusinessPartnerRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface BusinessPartnerRepository
        extends JpaRepository<BusinessPartner, Long> {

    @EntityGraph(attributePaths = "addresses")
    List<BusinessPartner> findByNameContainingIgnoreCase(
            String name
    );

    @EntityGraph(attributePaths = "addresses")
    List<BusinessPartner> findByRolesContaining(
            BusinessPartnerRole role
    );

    boolean existsByDocument(String document);

    boolean existsByDocumentAndIdNot(
            String document,
            Long id
    );
}
