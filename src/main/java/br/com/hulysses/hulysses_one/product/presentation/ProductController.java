package br.com.hulysses.hulysses_one.product.presentation;

import br.com.hulysses.hulysses_one.product.application.ProductService;
import br.com.hulysses.hulysses_one.product.presentation.dto.ProductResponse;
import br.com.hulysses.hulysses_one.product.presentation.dto.ProductRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Produtos", description = "Catálogo de produtos e seus fornecedores")
@RequestMapping("/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastrar registro")
    @ApiResponse(responseCode = "201", description = "Registro criado")
    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductRequest request
    ) {

        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> findAll() {

        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(@PathVariable Long id) {

        return ResponseEntity.ok(service.getById(id));
    }

    @Operation(summary = "Atualizar registro", description = "Substitui os campos e coleções informados; retorna 404 quando o ID não existe.")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(
                service.update(id, request)
        );
    }

    @Operation(summary = "Excluir registro", description = "Retorna 404 para ID inexistente e 409 se houver referências que impeçam a exclusão.")
    @ApiResponse(responseCode = "204", description = "Registro excluído", content = @Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    @Operation(summary = "Listar produtos ativos")
    public ResponseEntity<List<ProductResponse>> findActive() {

        return ResponseEntity.ok(service.findActive());
    }

    @GetMapping("/ordered-by-price")
    @Operation(summary = "Listar produtos por preço crescente")
    public ResponseEntity<List<ProductResponse>> findOrderedByPrice() {

        return ResponseEntity.ok(service.findAllOrderByPrice());
    }
}
