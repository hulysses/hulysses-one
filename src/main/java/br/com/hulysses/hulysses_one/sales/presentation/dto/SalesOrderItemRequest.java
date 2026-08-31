package br.com.hulysses.hulysses_one.sales.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SalesOrderItemRequest(

        @NotNull
        Long productId,

        @NotNull
        @Positive
        Integer quantity
) {
}