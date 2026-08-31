package br.com.hulysses.hulysses_one.product.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank
        String name,

        @NotBlank
        String description,

        @NotNull
        @Positive
        BigDecimal price,

        @NotNull
        Long supplierId,

        Boolean active
) {
}