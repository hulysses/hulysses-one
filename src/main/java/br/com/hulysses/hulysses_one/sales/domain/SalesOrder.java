package br.com.hulysses.hulysses_one.sales.domain;

import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartner;
import br.com.hulysses.hulysses_one.product.domain.exception.ProductException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SalesOrder {

    private Long id;
    private String orderNumber;
    private LocalDateTime orderDate;
    private String status;
    private BusinessPartner customer;
    private final List<SalesOrderProduct> products = new ArrayList<>();
    private Double totalAmount;

    public SalesOrder(Long id, String orderNumber, LocalDateTime orderDate, String status, BusinessPartner customer) {
        validate(orderNumber, orderDate, status, customer);
        this.id = id;
        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.status = status;
        this.customer = customer;
    }

    private void validate(String orderNumber, LocalDateTime orderDate, String status, BusinessPartner customer) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("Order number is required");
        }
        if (orderDate == null) {
            throw new IllegalArgumentException("Order date is required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required");
        }
    }

    public void addProduct(SalesOrderProduct product) {
        if (product == null) {
            throw new IllegalArgumentException("Product is required");
        }
        if (!product.getProduct().getActive()) {
            throw new ProductException("Product is inactive and cannot be added to the order");
        }

        this.products.add(product);
        calculateTotalAmount();
    }

    private void calculateTotalAmount() {
        this.totalAmount = products.stream()
                .mapToDouble(p -> p.getProduct().getPrice() * p.getQuantity())
                .sum();
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

    public Double getTotalAmount() {
        return totalAmount;
    }
}
