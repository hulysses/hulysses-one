package br.com.hulysses.hulysses_one.sales.presentation.dto;

import br.com.hulysses.hulysses_one.product.presentation.dto.ProductResponse;
import br.com.hulysses.hulysses_one.sales.domain.SalesOrderProduct;

public record SalesOrderProductResponse(Long id, ProductResponse product, Integer quantity) {
    public static SalesOrderProductResponse from(SalesOrderProduct item) {
        return new SalesOrderProductResponse(item.getId(), ProductResponse.from(item.getProduct()), item.getQuantity());
    }
}
