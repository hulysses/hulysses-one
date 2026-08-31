package br.com.hulysses.hulysses_one.businesspartner.domain;

import br.com.hulysses.hulysses_one.businesspartner.domain.exception.BusinessPartnerDocumentException;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "business_partner")
public class BusinessPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String document;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessPartnerType type;

    @OneToMany(
            mappedBy = "businessPartner",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Address> addresses = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "business_partner_role",
            joinColumns = @JoinColumn(name = "business_partner_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<BusinessPartnerRole> roles = new HashSet<>();

    protected BusinessPartner() {
    }

    public BusinessPartner(
            String name,
            String document,
            String email,
            String phone,
            BusinessPartnerType type
    ) {
        validate(name, document, type, email, phone);

        this.name = name;
        this.document = document;
        this.email = email;
        this.phone = phone;
        this.type = type;
    }

    public void update(
            String name,
            String document,
            String email,
            String phone,
            BusinessPartnerType type,
            Boolean active
    ) {
        validate(name, document, type, email, phone);

        this.name = name;
        this.document = document;
        this.email = email;
        this.phone = phone;
        this.type = type;
        this.isActive = active;
    }

    private void validate(
            String name,
            String document,
            BusinessPartnerType type,
            String email,
            String phone
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }

        if (type == null) {
            throw new IllegalArgumentException("Type is required");
        }

        validateDocument(document, type);

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone is required");
        }
    }

    private void validateDocument(
            String document,
            BusinessPartnerType type
    ) {
        if (document == null || document.isBlank()) {
            throw new IllegalArgumentException(
                    "Document is required"
            );
        }

        if (type == BusinessPartnerType.INDIVIDUAL
                && !document.matches("\\d{11}")) {

            throw new BusinessPartnerDocumentException(
                    "Invalid CPF format"
            );
        }

        if (type == BusinessPartnerType.COMPANY
                && !document.matches("\\d{14}")) {

            throw new BusinessPartnerDocumentException(
                    "Invalid CNPJ format"
            );
        }
    }

    public void addAddress(Address address) {
        if (address == null) {
            throw new IllegalArgumentException(
                    "Address is required"
            );
        }

        addresses.add(address);
        address.setBusinessPartner(this);
    }

    public void clearAddresses() {
        addresses.clear();
    }

    public void addRole(BusinessPartnerRole role) {
        if (role == null) {
            throw new IllegalArgumentException(
                    "Role is required"
            );
        }

        roles.add(role);
    }

    public void clearRoles() {
        roles.clear();
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

    public BusinessPartnerType getType() {
        return type;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public Set<BusinessPartnerRole> getRoles() {
        return roles;
    }
}