package br.com.hulysses.hulysses_one.product.domain;

import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartner;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "supplier_id",
            nullable = false
    )
    private BusinessPartner supplier;

    protected Product() {
    }

    public Product(
            String name,
            String description,
            BigDecimal price,
            BusinessPartner supplier
    ) {
        validate(
                name,
                description,
                price,
                supplier
        );

        this.name = name;
        this.description = description;
        this.price = price;
        this.supplier = supplier;
    }

    public void update(
            String name,
            String description,
            BigDecimal price,
            BusinessPartner supplier,
            Boolean active
    ) {
        validate(
                name,
                description,
                price,
                supplier
        );

        this.name = name;
        this.description = description;
        this.price = price;
        this.supplier = supplier;
        this.isActive = active;
    }

    public void validate(String name, String description, BigDecimal price, BusinessPartner supplier) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Name is required");
        }
        if (description == null || description.isBlank()) {
            throw new DomainException("Description is required");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Price must be greater than zero");
        }
        if (supplier == null) {
            throw new DomainException("Supplier is required");
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
                ", supplier=" + supplier +
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

    public BusinessPartner getSupplier() {
        return supplier;
    }
}
