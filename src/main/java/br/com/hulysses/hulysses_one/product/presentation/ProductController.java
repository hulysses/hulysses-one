package br.com.hulysses.hulysses_one.product.presentation;

import br.com.hulysses.hulysses_one.product.application.ProductService;
import br.com.hulysses.hulysses_one.product.domain.Product;
import br.com.hulysses.hulysses_one.product.presentation.dto.ProductRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Product> create(
            @Valid @RequestBody ProductRequest request
    ) {

        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<Product>> findAll() {

        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(@PathVariable Long id) {

        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(
                service.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    public ResponseEntity<List<Product>> findActive() {

        return ResponseEntity.ok(service.findActive());
    }

    @GetMapping("/ordered-by-price")
    public ResponseEntity<List<Product>> findOrderedByPrice() {

        return ResponseEntity.ok(service.findAllOrderByPrice());
    }
}
