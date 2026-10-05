package br.com.hulysses.hulysses_one.sales.presentation;

import br.com.hulysses.hulysses_one.sales.application.SalesOrderService;
import br.com.hulysses.hulysses_one.sales.presentation.dto.SalesOrderResponse;
import br.com.hulysses.hulysses_one.sales.presentation.dto.SalesOrderRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@Tag(name = "Pedidos de venda", description = "Pedidos, itens, consultas por cliente e status e total de vendas")
@RequestMapping("/sales-orders")
@ApiResponse(responseCode = "503", description = "Serviço de parceiros indisponível nas operações que consultam cliente/fornecedor",
        content = @Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                implementation = br.com.hulysses.hulysses_one.shared.exception.ApiError.class)))
public class SalesOrderController {

    private final SalesOrderService service;

    public SalesOrderController(SalesOrderService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastrar registro")
    @ApiResponse(responseCode = "201", description = "Registro criado")
    @PostMapping
    public ResponseEntity<SalesOrderResponse> create(
            @Valid @RequestBody SalesOrderRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<SalesOrderResponse>> findAll() {

        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesOrderResponse> findById(@PathVariable Long id) {

        return ResponseEntity.ok(service.getById(id));
    }

    @Operation(summary = "Atualizar registro", description = "Substitui os campos e coleções informados; retorna 404 quando o ID não existe.")
    @PutMapping("/{id}")
    public ResponseEntity<SalesOrderResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SalesOrderRequest request
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

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Listar pedidos de um cliente", description = "Retorna lista vazia quando não há pedidos para o ID informado.")
    public ResponseEntity<List<SalesOrderResponse>> findByCustomer(
            @Parameter(description = "ID do cliente") @PathVariable Long customerId) {

        return ResponseEntity.ok(service.findByCustomer(customerId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Buscar pedidos por status", description = "Comparação sem distinção entre maiúsculas e minúsculas.")
    public ResponseEntity<List<SalesOrderResponse>> findByStatus(
            @Parameter(description = "Status do pedido", example = "OPEN") @PathVariable String status) {

        return ResponseEntity.ok(service.findByStatus(status));
    }

    @GetMapping("/total")
    @Operation(summary = "Consultar o total de vendas", description = "Soma os totais persistidos de todos os pedidos, incluindo todos os status; retorna zero sem pedidos.")
    public ResponseEntity<BigDecimal> totalSales() {
        return ResponseEntity.ok(
                service.calculateTotalSales()
        );
    }
}
