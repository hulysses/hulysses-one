package br.com.hulysses.hulysses_one.businesspartner.integration.dto;

public record AddressResponse(Long id, String street, String number, String complement, String neighborhood,
                              String city, String state, String country, String postalCode) {}