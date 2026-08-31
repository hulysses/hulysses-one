package br.com.hulysses.hulysses_one.businesspartner.application;

import br.com.hulysses.hulysses_one.businesspartner.domain.Address;
import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartner;
import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartnerRole;
import br.com.hulysses.hulysses_one.businesspartner.persistence.BusinessPartnerRepository;
import br.com.hulysses.hulysses_one.businesspartner.presentation.dto.BusinessPartnerRequest;
import br.com.hulysses.hulysses_one.shared.domain.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusinessPartnerService {

    private final BusinessPartnerRepository repository;

    public BusinessPartnerService(
            BusinessPartnerRepository repository
    ) {
        this.repository = repository;
    }

    public BusinessPartner create(
            BusinessPartnerRequest request
    ) {

        if (repository.existsByDocument(
                request.document()
        )) {
            throw new IllegalArgumentException(
                    "Document already registered"
            );
        }

        BusinessPartner partner =
                new BusinessPartner(
                        request.name(),
                        request.document(),
                        request.email(),
                        request.phone(),
                        request.type()
                );

        if (request.addresses() != null) {
            request.addresses().forEach(addressRequest -> {
                Address address = new Address(
                        addressRequest.street(),
                        addressRequest.number(),
                        addressRequest.complement(),
                        addressRequest.neighborhood(),
                        addressRequest.city(),
                        addressRequest.state(),
                        addressRequest.country(),
                        addressRequest.postalCode()
                );

                partner.addAddress(address);
            });
        }

        request.roles()
                .forEach(partner::addRole);

        return repository.save(partner);
    }

    public BusinessPartner update(
            Long id,
            BusinessPartnerRequest request
    ) {

        BusinessPartner partner = findById(id);

        if (repository.existsByDocumentAndIdNot(
                request.document(),
                id
        )) {
            throw new IllegalArgumentException(
                    "Document already registered"
            );
        }

        partner.update(
                request.name(),
                request.document(),
                request.email(),
                request.phone(),
                request.type(),
                request.active() == null
                        ? partner.getActive()
                        : request.active()
        );

        partner.clearAddresses();

        if (request.addresses() != null) {
            request.addresses().forEach(addressRequest -> {
                Address address = new Address(
                        addressRequest.street(),
                        addressRequest.number(),
                        addressRequest.complement(),
                        addressRequest.neighborhood(),
                        addressRequest.city(),
                        addressRequest.state(),
                        addressRequest.country(),
                        addressRequest.postalCode()
                );

                partner.addAddress(address);
            });
        }

        partner.clearRoles();

        request.roles()
                .forEach(partner::addRole);

        return repository.save(partner);
    }

    public void delete(Long id) {
        findById(id);

        repository.deleteById(id);
    }

    public BusinessPartner findById(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Business partner",
                                id
                        )
                );
    }

    public List<BusinessPartner> findAll() {
        return repository.findAll();
    }

    public List<BusinessPartner> findCustomers() {
        return repository.findByRolesContaining(
                BusinessPartnerRole.CUSTOMER
        );
    }

    public List<BusinessPartner> findSuppliers() {
        return repository.findByRolesContaining(
                BusinessPartnerRole.SUPPLIER
        );
    }

    public List<BusinessPartner> findByName(
            String name
    ) {
        return repository
                .findByNameContainingIgnoreCase(name);
    }
}