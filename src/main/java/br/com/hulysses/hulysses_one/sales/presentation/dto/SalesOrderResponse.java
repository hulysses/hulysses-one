package br.com.hulysses.hulysses_one.sales.presentation.dto;

import br.com.hulysses.hulysses_one.businesspartner.presentation.dto.BusinessPartnerResponse;
import br.com.hulysses.hulysses_one.sales.domain.SalesOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SalesOrderResponse(Long id, String orderNumber, LocalDateTime orderDate, String status,
                                 BusinessPartnerResponse customer, List<SalesOrderProductResponse> products,
                                 BigDecimal totalAmount) {
    public static SalesOrderResponse from(SalesOrder order) {
        return new SalesOrderResponse(order.getId(), order.getOrderNumber(), order.getOrderDate(), order.getStatus(),
                BusinessPartnerResponse.from(order.getCustomer()),
                order.getProducts().stream().map(SalesOrderProductResponse::from).toList(), order.getTotalAmount());
    }
}
