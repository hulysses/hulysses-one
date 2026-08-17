package br.com.hulysses.hulysses_one.sales.domain;

import br.com.hulysses.hulysses_one.product.domain.Product;

public class SalesOrderProduct {

    private Long id;
    private SalesOrder salesOrder;
    private Product product;
    private Integer quantity;

    public SalesOrderProduct(Long id, SalesOrder salesOrder, Product product, Integer quantity) {
        validate(salesOrder, product, quantity);
        this.id = id;
        this.salesOrder = salesOrder;
        this.product = product;
        this.quantity = quantity;
    }

    private void validate(SalesOrder salesOrder, Product product, Integer quantity) {
        if (salesOrder == null) {
            throw new IllegalArgumentException("Sales order is required");
        }
        if (product == null) {
            throw new IllegalArgumentException("Product is required");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

    @Override
    public String toString() {
        return "SalesOrderProduct{" +
                "id=" + id +
                ", salesOrder=" + salesOrder +
                ", product=" + product +
                ", quantity=" + quantity +
                '}';
    }

    public Long getId() {
        return id;
    }

    public SalesOrder getSalesOrder() {
        return salesOrder;
    }

    public Product getProduct() {
        return product;
    }

    public Integer getQuantity() {
        return quantity;
    }
}