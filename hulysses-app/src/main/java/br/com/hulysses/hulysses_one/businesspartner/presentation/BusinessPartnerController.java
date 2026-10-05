package br.com.hulysses.hulysses_one.businesspartner.presentation;

import br.com.hulysses.hulysses_one.businesspartner.application.BusinessPartnerService;
import br.com.hulysses.hulysses_one.businesspartner.integration.dto.BusinessPartnerResponse;
import br.com.hulysses.hulysses_one.businesspartner.integration.dto.BusinessPartnerRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Parceiros de negócio", description = "Clientes, fornecedores e demais papéis de parceiros, incluindo endereços")
@RequestMapping("/business-partners")
@ApiResponse(responseCode = "503", description = "Serviço de parceiros indisponível",
        content = @Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                implementation = br.com.hulysses.hulysses_one.shared.exception.ApiError.class)))
public class BusinessPartnerController {

    private final BusinessPartnerService service;

    public BusinessPartnerController(BusinessPartnerService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastrar registro")
    @ApiResponse(responseCode = "201", description = "Registro criado")
    @PostMapping
    public ResponseEntity<BusinessPartnerResponse> create(
            @Valid @RequestBody BusinessPartnerRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<BusinessPartnerResponse>> findAll() {

        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessPartnerResponse> findById(@PathVariable Long id) {

        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/customers")
    @Operation(summary = "Listar parceiros com papel CUSTOMER")
    public ResponseEntity<List<BusinessPartnerResponse>> findCustomers() {

        return ResponseEntity.ok(
                service.findCustomers()
        );
    }

    @GetMapping("/suppliers")
    @Operation(summary = "Listar parceiros com papel SUPPLIER")
    public ResponseEntity<List<BusinessPartnerResponse>> findSuppliers() {

        return ResponseEntity.ok(
                service.findSuppliers()
        );
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar parceiros por parte do nome", description = "Busca sem distinção entre maiúsculas e minúsculas.")
    public ResponseEntity<List<BusinessPartnerResponse>> findByName(
            @Parameter(description = "Trecho do nome do parceiro", example = "Maria") @RequestParam String name
    ) {

        return ResponseEntity.ok(
                service.findByName(name)
        );
    }

    @Operation(summary = "Atualizar registro", description = "Substitui os campos e coleções informados; retorna 404 quando o ID não existe.")
    @PutMapping("/{id}")
    public ResponseEntity<BusinessPartnerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BusinessPartnerRequest request
    ) {

        return ResponseEntity.ok(
                service.update(id, request)
        );
    }

    @Operation(summary = "Excluir registro", description = "Retorna 404 para ID inexistente. "
            + "Referências de produtos e vendas são IDs remotos e não impedem a exclusão; prefira inativação.")
    @ApiResponse(responseCode = "204", description = "Registro excluído", content = @Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
