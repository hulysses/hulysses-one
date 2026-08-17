package br.com.hulysses.hulysses_one.businesspartner.domain;

import br.com.hulysses.hulysses_one.businesspartner.domain.exception.AddressException;

public class Address {

    private Long id;
    private String street;
    private String number;
    private String complement;
    private String neighborhood;
    private String city;
    private String state;
    private String country;
    private String postalCode;

    public Address(Long id, String street, String number, String complement, String neighborhood, String city,
                      String state, String country, String postalCode) {

        validate(street, number, neighborhood, city, state, country, postalCode);
        this.id = id;
        this.street = street;
        this.number = number;
        this.complement = complement;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.country = country;
        this.postalCode = postalCode;
    }

    public void validate(String street, String number, String neighborhood, String city, String state, String country,
                         String postalCode) {

        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("Street is required");
        }
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException("Number is required");
        }
        if (neighborhood == null || neighborhood.isBlank()) {
            throw new IllegalArgumentException("Neighborhood is required");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City is required");
        }
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("State is required");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country is required");
        }
        validatePostalCode(postalCode);
    }

    public void validatePostalCode(String postalCode) {
        if (postalCode == null || postalCode.isBlank()) {
            throw new IllegalArgumentException("Postal code is required");
        }
        if (!postalCode.matches("\\d{8}")) {
            throw new AddressException("Invalid postal code format");
        }
    }

    @Override
    public String toString() {
        return "Address{" +
                "id=" + id +
                ", street='" + street + '\'' +
                ", number='" + number + '\'' +
                ", complement='" + complement + '\'' +
                ", neighborhood='" + neighborhood + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", country='" + country + '\'' +
                ", postalCode='" + postalCode + '\'' +
                '}';
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
