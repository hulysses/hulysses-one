package br.com.hulysses.hulysses_one.product.application;

import br.com.hulysses.hulysses_one.product.domain.Product;
import br.com.hulysses.hulysses_one.shared.domain.DuplicateEntityException;
import br.com.hulysses.hulysses_one.shared.domain.EntityNotFoundException;

import java.util.*;

public class ProductService {

    private final Map<Long, Product> products = new HashMap<>();

    public Product create(Product product) {
        validateProduct(product);

        if (products.containsKey(product.getId())) {
            throw new DuplicateEntityException(
                    "Product",
                    product.getId()
            );
        }

        products.put(product.getId(), product);

        return product;
    }

    public Product update(Product product) {
        validateProduct(product);

        findById(product.getId());

        products.put(product.getId(), product);

        return product;
    }

    public void delete(Long id) {
        findById(id);

        products.remove(id);
    }

    public Product findById(Long id) {
        validateId(id);

        Product product = products.get(id);

        if (product == null) {
            throw new EntityNotFoundException(
                    "Product",
                    id
            );
        }

        return product;
    }

    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    public List<Product> findActive() {

        return products.values()
                .stream()
                .filter(Product::getActive)
                .toList();
    }

    public Optional<Product> findByName(String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Name is required"
            );
        }

        return products.values()
                .stream()
                .filter(product ->
                        product.getName()
                                .equalsIgnoreCase(name.trim())
                )
                .findFirst();
    }

    public List<Product> findAllOrderByPrice() {

        return products.values()
                .stream()
                .sorted(
                        Comparator.comparing(Product::getPrice)
                )
                .toList();
    }

    public List<Product> findAllOrderByName() {

        return products.values()
                .stream()
                .sorted(
                        Comparator.comparing(
                                Product::getName,
                                String.CASE_INSENSITIVE_ORDER
                        )
                )
                .toList();
    }

    public List<String> findProductNames() {

        return products.values()
                .stream()
                .map(Product::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private void validateProduct(Product product) {

        if (product == null) {
            throw new IllegalArgumentException(
                    "Product is required"
            );
        }

        validateId(product.getId());
    }

    private void validateId(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Id must be greater than zero"
            );
        }
    }
}