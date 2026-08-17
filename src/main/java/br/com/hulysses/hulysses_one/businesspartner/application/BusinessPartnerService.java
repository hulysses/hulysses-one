package br.com.hulysses.hulysses_one.businesspartner.application;

import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartner;
import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartnerRole;
import br.com.hulysses.hulysses_one.shared.domain.DuplicateEntityException;
import br.com.hulysses.hulysses_one.shared.domain.EntityNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BusinessPartnerService {

    private final Map<Long, BusinessPartner> partners = new HashMap<>();

    public BusinessPartner create(BusinessPartner partner) {
        validatePartner(partner);

        if (partners.containsKey(partner.getId())) {
            throw new DuplicateEntityException(
                    "Business partner",
                    partner.getId()
            );
        }

        partners.put(partner.getId(), partner);

        return partner;
    }

    public BusinessPartner update(BusinessPartner partner) {
        validatePartner(partner);
        findById(partner.getId());
        partners.put(partner.getId(), partner);

        return partner;
    }

    public void delete(Long id) {
        findById(id);
        partners.remove(id);
    }

    public BusinessPartner findById(Long id) {
        validateId(id);
        BusinessPartner partner = partners.get(id);

        if (partner == null) {
            throw new EntityNotFoundException(
                    "Business partner",
                    id
            );
        }

        return partner;
    }

    public List<BusinessPartner> findAll() {
        return new ArrayList<>(partners.values());
    }

    public List<BusinessPartner> findByRole(
            BusinessPartnerRole role
    ) {

        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }

        return partners.values()
                .stream()
                .filter(partner ->
                        partner.getRoles().contains(role)
                )
                .toList();
    }

    public List<BusinessPartner> findCustomers() {
        return findByRole(BusinessPartnerRole.CUSTOMER);
    }

    public List<BusinessPartner> findSuppliers() {
        return findByRole(BusinessPartnerRole.SUPPLIER);
    }

    public List<BusinessPartner> findByName(String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Name is required"
            );
        }

        String search = name
                .trim()
                .toLowerCase();

        return partners.values()
                .stream()
                .filter(partner ->
                        partner.getName()
                                .toLowerCase()
                                .contains(search)
                )
                .toList();
    }

    public List<BusinessPartner> findAllOrderByName() {

        return partners.values()
                .stream()
                .sorted(
                        Comparator.comparing(
                                BusinessPartner::getName,
                                String.CASE_INSENSITIVE_ORDER
                        )
                )
                .toList();
    }

    public List<String> findAllNames() {

        return partners.values()
                .stream()
                .map(BusinessPartner::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private void validatePartner(BusinessPartner partner) {

        if (partner == null) {
            throw new IllegalArgumentException(
                    "Business partner is required"
            );
        }

        validateId(partner.getId());
    }

    private void validateId(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Id must be greater than zero"
            );
        }
    }
}