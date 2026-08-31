package br.com.hulysses.hulysses_one.businesspartner.domain;

import br.com.hulysses.hulysses_one.businesspartner.domain.exception.BusinessPartnerDocumentException;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.ArrayList;
import java.util.List;

public class BusinessPartner {

    private Long id;
    private String name;
    private String document;
    private String email;
    private String phone;
    private Boolean isActive = Boolean.TRUE;
    private BusinessPartnerType type;
    private final List<Address> addresses = new ArrayList<>();
    private final List<BusinessPartnerRole> roles = new ArrayList<>();

    public BusinessPartner(Long id, String name, String document, String email, String phone,
                           BusinessPartnerType type) {
        validate(name, document, type, email, phone);
        this.id = id;
        this.name = name;
        this.document = document;
        this.email = email;
        this.phone = phone;
        this.type = type;
    }

    private void validate(String name, String document, BusinessPartnerType type, String email, String phone) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        validateDocument(document, type);
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type is required");
        }
    }

    private void validateDocument(String document, BusinessPartnerType type) {
        if (document == null || document.isBlank()) {
            throw new IllegalArgumentException("Document is required");
        }
        if (type == BusinessPartnerType.INDIVIDUAL) {
            if (!document.matches("\\d{11}")) {
                throw new BusinessPartnerDocumentException("Invalid CPF format");
            }
        } else if (type == BusinessPartnerType.COMPANY) {
            if (!document.matches("\\d{14}")) {
                throw new BusinessPartnerDocumentException("Invalid CNPJ format");
            }
        }
    }

    public void addAddress(Address address) {
        if (address == null) {
            throw new IllegalArgumentException("Address is required");
        }
        this.addresses.add(address);
    }

    public void addRole(BusinessPartnerRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }

        if (!roles.contains(role)) {
            roles.add(role);
        }
    }

    @Override
    public String toString() {
        return "BusinessPartner{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", document='" + document + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", isActive=" + isActive +
                ", type=" + type +
                ", roles=" + roles +
                '}';
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDocument() {
        return document;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public Boolean getActive() {
        return isActive;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public BusinessPartnerType getType() {
        return type;
    }

    public List<BusinessPartnerRole> getRoles() {
        return roles;
    }

    @JsonSetter("roles")
    public void setRoles(List<BusinessPartnerRole> roles) {

        this.roles.clear();

        if (roles != null) {
            roles.forEach(this::addRole);
        }
    }

    @JsonSetter("addresses")
    public void setAddresses(List<Address> addresses) {

        this.addresses.clear();

        if (addresses != null) {
            addresses.forEach(this::addAddress);
        }
    }
}
