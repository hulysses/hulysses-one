package br.com.hulysses.hulysses_one.businesspartner.presentation.dto;

import br.com.hulysses.hulysses_one.businesspartner.domain.Address;

public record AddressResponse(Long id, String street, String number, String complement,
                              String neighborhood, String city, String state, String country, String postalCode) {
    public static AddressResponse from(Address address) {
        return new AddressResponse(address.getId(), address.getStreet(), address.getNumber(), address.getComplement(),
                address.getNeighborhood(), address.getCity(), address.getState(), address.getCountry(), address.getPostalCode());
    }
}
