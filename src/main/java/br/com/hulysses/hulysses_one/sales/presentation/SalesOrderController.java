package br.com.hulysses.hulysses_one.sales.presentation;

import br.com.hulysses.hulysses_one.sales.application.SalesOrderService;
import br.com.hulysses.hulysses_one.sales.domain.SalesOrder;
import br.com.hulysses.hulysses_one.sales.presentation.dto.SalesOrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/sales-orders")
public class SalesOrderController {

    private final SalesOrderService service;

    public SalesOrderController(SalesOrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SalesOrder> create(
            @Valid @RequestBody SalesOrderRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<SalesOrder>> findAll() {

        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesOrder> findById(@PathVariable Long id) {

        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalesOrder> update(
            @PathVariable Long id,
            @Valid @RequestBody SalesOrderRequest request
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

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<SalesOrder>> findByCustomer(@PathVariable Long customerId) {

        return ResponseEntity.ok(service.findByCustomer(customerId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<SalesOrder>> findByStatus(@PathVariable String status) {

        return ResponseEntity.ok(service.findByStatus(status));
    }

    @GetMapping("/total")
    public ResponseEntity<BigDecimal> totalSales() {
        return ResponseEntity.ok(
                service.calculateTotalSales()
        );
    }
}