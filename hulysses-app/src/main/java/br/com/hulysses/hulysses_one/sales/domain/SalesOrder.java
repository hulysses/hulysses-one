package br.com.hulysses.hulysses_one.sales.domain;

import jakarta.persistence.*;
import br.com.hulysses.hulysses_one.shared.exception.DomainException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_order")
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            unique = true
    )
    private String orderNumber;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false)
    private String status;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @OneToMany(
            mappedBy = "salesOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<SalesOrderProduct> products =
            new ArrayList<>();

    @Column(
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal totalAmount = BigDecimal.ZERO;

    protected SalesOrder() {
    }

    public SalesOrder(
            String orderNumber,
            LocalDateTime orderDate,
            String status,
            Long customerId
    ) {
        validate(
                orderNumber,
                orderDate,
                status,
                customerId
        );

        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.status = status;
        this.customerId = customerId;
    }

    private void validate(String orderNumber, LocalDateTime orderDate, String status, Long customerId) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new DomainException("Order number is required");
        }
        if (orderDate == null) {
            throw new DomainException("Order date is required");
        }
        if (status == null || status.isBlank()) {
            throw new DomainException("Status is required");
        }
        if (customerId == null) {
            throw new DomainException("customerId is required");
        }
    }

    public void update(
            String orderNumber,
            LocalDateTime orderDate,
            String status,
            Long customerId
    ) {
        validate(
                orderNumber,
                orderDate,
                status,
                customerId
        );

        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.status = status;
        this.customerId = customerId;
    }

    public void clearProducts() {
        products.clear();
        calculateTotalAmount();
    }

    public void addProduct(SalesOrderProduct product) {
        if (product == null) {
            throw new DomainException("Product is required");
        }
        this.products.add(product);
        calculateTotalAmount();
    }

    private void calculateTotalAmount() {
        this.totalAmount = products.stream()
                .map(p -> p.getProduct()
                        .getPrice()
                        .multiply(BigDecimal.valueOf(p.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public String toString() {
        return "SalesOrder{" +
                "id=" + id +
                ", orderNumber='" + orderNumber + '\'' +
                ", orderDate=" + orderDate +
                ", status='" + status + '\'' +
                ", customerId=" + customerId +
                ", products=" + products +
                ", totalAmount=" + totalAmount +
                '}';
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public String getStatus() {
        return status;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public List<SalesOrderProduct> getProducts() {
        return products;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
