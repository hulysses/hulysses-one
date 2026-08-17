package br.com.hulysses.hulysses_one.product.domain;

import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartner;

public class Product {

    private Long id;
    private String name;
    private String description;
    private Double price;
    private Boolean isActive = Boolean.TRUE;

    // manter enquanto nao implementa a tabela de precos e a tabela de relacionamento de produtos com fornecedores
    private BusinessPartner supplier;

    public Product(Long id, String name, String description, Double price, BusinessPartner supplier) {
        validate(name, description, price, supplier);
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.supplier = supplier;
    }

    public void validate(String name, String description, Double price, BusinessPartner supplier) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (price == null || price <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier is required");
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

    public Double getPrice() {
        return price;
    }

    public Boolean getActive() {
        return isActive;
    }

    public BusinessPartner getSupplier() {
        return supplier;
    }
}
