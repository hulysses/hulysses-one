package br.com.hulysses.hulysses_one.businesspartner.persistence;

import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartner;
import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartnerRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessPartnerRepository
        extends JpaRepository<BusinessPartner, Long> {

    List<BusinessPartner> findByNameContainingIgnoreCase(
            String name
    );

    List<BusinessPartner> findByRolesContaining(
            BusinessPartnerRole role
    );

    boolean existsByDocument(String document);

    boolean existsByDocumentAndIdNot(
            String document,
            Long id
    );
}