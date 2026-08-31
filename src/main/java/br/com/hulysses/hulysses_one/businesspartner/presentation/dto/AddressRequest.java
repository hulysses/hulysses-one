package br.com.hulysses.hulysses_one.businesspartner.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(

        @NotBlank String street,

        @NotBlank String number,

        String complement,

        @NotBlank String neighborhood,

        @NotBlank String city,

        @NotBlank String state,

        @NotBlank String country,

        @NotBlank String postalCode) {
}