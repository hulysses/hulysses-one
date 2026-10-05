package br.com.hulysses.business_partner_service.businesspartner.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record AddressRequest(

        @NotBlank @Size(max = 255) String street,

        @NotBlank @Size(max = 255) String number,

        @Size(max = 255) String complement,

        @NotBlank @Size(max = 255) String neighborhood,

        @NotBlank @Size(max = 255) String city,

        @NotBlank @Size(max = 255) String state,

        @NotBlank @Size(max = 255) String country,

        @NotBlank @Pattern(regexp = "\\d{8}", message = "Postal code must contain 8 digits")
        @Schema(description = "CEP sem pontuação", example = "01001000") String postalCode) {
}
