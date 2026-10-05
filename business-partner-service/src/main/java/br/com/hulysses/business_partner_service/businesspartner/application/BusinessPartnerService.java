package br.com.hulysses.business_partner_service.businesspartner.application;

import br.com.hulysses.business_partner_service.businesspartner.domain.Address;
import br.com.hulysses.business_partner_service.businesspartner.domain.BusinessPartner;
import br.com.hulysses.business_partner_service.businesspartner.domain.BusinessPartnerRole;
import br.com.hulysses.business_partner_service.businesspartner.persistence.BusinessPartnerRepository;
import br.com.hulysses.business_partner_service.businesspartner.presentation.dto.BusinessPartnerRequest;
import br.com.hulysses.business_partner_service.businesspartner.presentation.dto.BusinessPartnerResponse;
import br.com.hulysses.business_partner_service.shared.exception.DomainException;
import br.com.hulysses.business_partner_service.shared.exception.DuplicateEntityException;
import br.com.hulysses.business_partner_service.shared.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BusinessPartnerService {

    private final BusinessPartnerRepository repository;

    public BusinessPartnerService(
            BusinessPartnerRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public BusinessPartnerResponse create(
            BusinessPartnerRequest request
    ) {

        if (repository.existsByDocument(
                request.document()
        )) {
            throw new DuplicateEntityException(
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

        return BusinessPartnerResponse.from(repository.save(partner));
    }

    @Transactional
    public BusinessPartnerResponse update(
            Long id,
            BusinessPartnerRequest request
    ) {

        BusinessPartner partner = findById(id);

        if (repository.existsByDocumentAndIdNot(
                request.document(),
                id
        )) {
            throw new DuplicateEntityException(
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

        return BusinessPartnerResponse.from(repository.save(partner));
    }

    @Transactional
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

    public BusinessPartnerResponse getById(Long id) {
        return BusinessPartnerResponse.from(findById(id));
    }

    public BusinessPartner findSupplierById(Long id) {
        return findByRole(id, BusinessPartnerRole.SUPPLIER, "Supplier");
    }

    public BusinessPartner findCustomerById(Long id) {
        return findByRole(id, BusinessPartnerRole.CUSTOMER, "Customer");
    }

    public BusinessPartnerResponse getEligibleById(Long id, BusinessPartnerRole role) {
        return BusinessPartnerResponse.from(findByRole(id, role, role.name()));
    }

    private BusinessPartner findByRole(Long id, BusinessPartnerRole role, String resource) {
        BusinessPartner partner = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(resource, id));
        if (!partner.getRoles().contains(role)) {
            throw new DomainException("Business partner is not a " + resource.toLowerCase(java.util.Locale.ROOT));
        }
        return partner;
    }

    public List<BusinessPartnerResponse> findAll() {
        return repository.findAll().stream().map(BusinessPartnerResponse::from).toList();
    }

    public List<BusinessPartnerResponse> findCustomers() {
        return repository.findByRolesContaining(
                BusinessPartnerRole.CUSTOMER
        ).stream().map(BusinessPartnerResponse::from).toList();
    }

    public List<BusinessPartnerResponse> findSuppliers() {
        return repository.findByRolesContaining(
                BusinessPartnerRole.SUPPLIER
        ).stream().map(BusinessPartnerResponse::from).toList();
    }

    public List<BusinessPartnerResponse> findByName(
            String name
    ) {
        return repository
                .findByNameContainingIgnoreCase(name).stream().map(BusinessPartnerResponse::from).toList();
    }
}
