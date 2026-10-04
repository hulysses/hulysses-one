package br.com.hulysses.hulysses_one.product.persistence;

import br.com.hulysses.hulysses_one.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "supplier")
    List<Product> findByIsActiveTrue();

    Optional<Product> findByNameIgnoreCase(
            String name
    );

    @EntityGraph(attributePaths = "supplier")
    List<Product> findAllByOrderByPriceAsc();
}
