package br.com.hulysses.hulysses_one.businesspartner.presentation;

import br.com.hulysses.hulysses_one.businesspartner.application.BusinessPartnerService;
import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartner;
import br.com.hulysses.hulysses_one.businesspartner.presentation.dto.BusinessPartnerRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/business-partners")
public class BusinessPartnerController {

    private final BusinessPartnerService service;

    public BusinessPartnerController(BusinessPartnerService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<BusinessPartner> create(
            @Valid @RequestBody BusinessPartnerRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<BusinessPartner>> findAll() {

        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessPartner> findById(@PathVariable Long id) {

        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/customers")
    public ResponseEntity<List<BusinessPartner>> findCustomers() {

        return ResponseEntity.ok(
                service.findCustomers()
        );
    }

    @GetMapping("/suppliers")
    public ResponseEntity<List<BusinessPartner>> findSuppliers() {

        return ResponseEntity.ok(
                service.findSuppliers()
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<BusinessPartner>> findByName(
            @RequestParam String name
    ) {

        return ResponseEntity.ok(
                service.findByName(name)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<BusinessPartner> update(
            @PathVariable Long id,
            @Valid @RequestBody BusinessPartnerRequest request
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
}