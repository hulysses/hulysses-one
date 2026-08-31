package br.com.hulysses.hulysses_one.businesspartner.presentation.dto;

import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartnerRole;
import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartnerType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

public record BusinessPartnerRequest(

        @NotBlank
        String name,

        @NotBlank
        String document,

        @NotBlank
        @Email
        String email,

        @NotBlank
        String phone,

        @NotNull
        BusinessPartnerType type,

        @NotEmpty
        Set<BusinessPartnerRole> roles,

        List<@Valid AddressRequest> addresses,

        Boolean active

) {
}