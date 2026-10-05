package br.com.hulysses.hulysses_one.product.domain;

import jakarta.persistence.*;
import br.com.hulysses.hulysses_one.shared.exception.DomainException;

import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal price;

    @Column(nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    protected Product() {
    }

    public Product(
            String name,
            String description,
            BigDecimal price,
            Long supplierId
    ) {
        validate(
                name,
                description,
                price,
                supplierId
        );

        this.name = name;
        this.description = description;
        this.price = price;
        this.supplierId = supplierId;
    }

    public void update(
            String name,
            String description,
            BigDecimal price,
            Long supplierId,
            Boolean active
    ) {
        validate(
                name,
                description,
                price,
                supplierId
        );

        this.name = name;
        this.description = description;
        this.price = price;
        this.supplierId = supplierId;
        this.isActive = active;
    }

    public void validate(String name, String description, BigDecimal price, Long supplierId) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Name is required");
        }
        if (description == null || description.isBlank()) {
            throw new DomainException("Description is required");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Price must be greater than zero");
        }
        if (supplierId == null) {
            throw new DomainException("supplierId is required");
        }
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", isActive=" + isActive +
                ", supplierId=" + supplierId +
                '}';
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Boolean getActive() {
        return isActive;
    }

    public Long getSupplierId() {
        return supplierId;
    }
}
