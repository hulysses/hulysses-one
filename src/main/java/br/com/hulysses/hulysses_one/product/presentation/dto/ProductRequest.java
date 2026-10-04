package br.com.hulysses.hulysses_one.product.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank
        @Size(max = 255)
        String name,

        @NotBlank
        @Size(max = 255)
        String description,

        @NotNull
        @Positive
        @Digits(integer = 13, fraction = 2)
        BigDecimal price,

        @NotNull
        @Positive
        @Schema(description = "ID de um parceiro com papel SUPPLIER")
        Long supplierId,

        @Schema(description = "Aplicado na atualização; ausente mantém o estado atual. Novos produtos são ativos.")
        Boolean active
) {
}
