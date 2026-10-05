package br.com.hulysses.hulysses_one.sales.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record SalesOrderRequest(

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Número único do pedido", example = "VENDA-001")
        String orderNumber,

        @NotNull
        LocalDateTime orderDate,

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Status informado pelo negócio; o modelo atual não impõe uma lista fechada", example = "OPEN")
        String status,

        @NotNull
        @Positive
        @Schema(description = "ID de um parceiro com papel CUSTOMER")
        Long customerId,

        @NotEmpty
        List<@NotNull @Valid SalesOrderItemRequest> items
) {
}
