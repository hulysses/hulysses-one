package br.com.hulysses.hulysses_one.product.persistence;

import br.com.hulysses.hulysses_one.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    List<Product> findByIsActiveTrue();

    Optional<Product> findByNameIgnoreCase(
            String name
    );

    List<Product> findAllByOrderByPriceAsc();
}
