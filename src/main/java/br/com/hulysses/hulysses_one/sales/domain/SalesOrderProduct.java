package br.com.hulysses.hulysses_one.sales.domain;

import br.com.hulysses.hulysses_one.product.domain.Product;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "sales_order_product")
public class SalesOrderProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "sales_order_id",
            nullable = false
    )
    @JsonIgnore
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    protected SalesOrderProduct() {
    }

    public SalesOrderProduct(
            SalesOrder salesOrder,
            Product product,
            Integer quantity
    ) {
        validate(
                salesOrder,
                product,
                quantity
        );

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
                ", salesOrderId=" +
                (salesOrder != null
                        ? salesOrder.getId()
                        : null) +
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