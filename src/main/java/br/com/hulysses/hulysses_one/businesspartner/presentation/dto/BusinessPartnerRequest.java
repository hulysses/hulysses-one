package br.com.hulysses.hulysses_one.businesspartner.presentation.dto;

import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartnerRole;
import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartnerType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.hulysses.hulysses_one.businesspartner.presentation.validation.ValidPartnerDocument;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Set;

@ValidPartnerDocument
public record BusinessPartnerRequest(

        @NotBlank
        @Size(max = 255)
        String name,

        @NotBlank
        @Schema(description = "CPF com 11 dígitos para INDIVIDUAL; CNPJ com 14 para COMPANY, sem pontuação", example = "12345678901")
        String document,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(max = 255)
        String phone,

        @NotNull
        BusinessPartnerType type,

        @NotEmpty
        Set<@NotNull BusinessPartnerRole> roles,

        List<@NotNull @Valid AddressRequest> addresses,

        @Schema(description = "Aplicado na atualização; ausente mantém o estado atual. Novos parceiros são ativos.")
        Boolean active

) {
}
