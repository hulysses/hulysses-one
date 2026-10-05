package br.com.hulysses.hulysses_one.product.presentation.dto;

import br.com.hulysses.hulysses_one.businesspartner.integration.dto.BusinessPartnerResponse;
import br.com.hulysses.hulysses_one.product.domain.Product;
import java.math.BigDecimal;

public record ProductResponse(Long id, String name, String description, BigDecimal price,
                              Boolean active, BusinessPartnerResponse supplier) {
    public static ProductResponse from(Product product, BusinessPartnerResponse supplier) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getActive(), supplier);
    }
}
