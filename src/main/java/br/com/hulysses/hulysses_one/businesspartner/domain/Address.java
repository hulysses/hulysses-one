package br.com.hulysses.hulysses_one.businesspartner.domain;

import br.com.hulysses.hulysses_one.businesspartner.domain.exception.AddressException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import br.com.hulysses.hulysses_one.shared.exception.DomainException;

@Entity
@Table(name = "address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String number;

    private String complement;

    @Column(nullable = false)
    private String neighborhood;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String postalCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_partner_id", nullable = false)
    @JsonIgnore
    private BusinessPartner businessPartner;

    protected Address() {
    }

    public Address(String street, String number, String complement, String neighborhood, String city, String state, String country, String postalCode) {
        validate(street, number, neighborhood, city, state, country, postalCode);

        this.street = street;
        this.number = number;
        this.complement = complement;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.country = country;
        this.postalCode = postalCode;
    }

    void setBusinessPartner(BusinessPartner businessPartner) {
        this.businessPartner = businessPartner;
    }

    public void validate(String street, String number, String neighborhood, String city, String state, String country, String postalCode) {

        if (street == null || street.isBlank()) {
            throw new DomainException("Street is required");
        }
        if (number == null || number.isBlank()) {
            throw new DomainException("Number is required");
        }
        if (neighborhood == null || neighborhood.isBlank()) {
            throw new DomainException("Neighborhood is required");
        }
        if (city == null || city.isBlank()) {
            throw new DomainException("City is required");
        }
        if (state == null || state.isBlank()) {
            throw new DomainException("State is required");
        }
        if (country == null || country.isBlank()) {
            throw new DomainException("Country is required");
        }
        validatePostalCode(postalCode);
    }

    public void validatePostalCode(String postalCode) {
        if (postalCode == null || postalCode.isBlank()) {
            throw new DomainException("Postal code is required");
        }
        if (!postalCode.matches("\\d{8}")) {
            throw new AddressException("Invalid postal code format");
        }
    }

    @Override
    public String toString() {
        return "Address{" + "id=" + id + ", street='" + street + '\'' + ", number='" + number + '\'' + ", complement='" + complement + '\'' + ", neighborhood='" + neighborhood + '\'' + ", city='" + city + '\'' + ", state='" + state + '\'' + ", country='" + country + '\'' + ", postalCode='" + postalCode + '\'' + '}';
    }

    public Long getId() {
        return id;
    }

    public String getStreet() {
        return street;
    }

    public String getNumber() {
        return number;
    }

    public String getComplement() {
        return complement;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getCountry() {
        return country;
    }

    public String getPostalCode() {
        return postalCode;
    }
}