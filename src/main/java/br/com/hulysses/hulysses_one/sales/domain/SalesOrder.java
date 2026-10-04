package br.com.hulysses.hulysses_one.sales.domain;

import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartner;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "customer_id",
            nullable = false
    )
    private BusinessPartner customer;

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
            BusinessPartner customer
    ) {
        validate(
                orderNumber,
                orderDate,
                status,
                customer
        );

        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.status = status;
        this.customer = customer;
    }

    private void validate(String orderNumber, LocalDateTime orderDate, String status, BusinessPartner customer) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new DomainException("Order number is required");
        }
        if (orderDate == null) {
            throw new DomainException("Order date is required");
        }
        if (status == null || status.isBlank()) {
            throw new DomainException("Status is required");
        }
        if (customer == null) {
            throw new DomainException("Customer is required");
        }
    }

    public void update(
            String orderNumber,
            LocalDateTime orderDate,
            String status,
            BusinessPartner customer
    ) {
        validate(
                orderNumber,
                orderDate,
                status,
                customer
        );

        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.status = status;
        this.customer = customer;
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
                ", customer=" + customer +
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

    public BusinessPartner getCustomer() {
        return customer;
    }

    public List<SalesOrderProduct> getProducts() {
        return products;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
