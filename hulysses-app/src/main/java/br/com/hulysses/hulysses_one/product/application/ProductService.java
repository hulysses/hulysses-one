package br.com.hulysses.hulysses_one.product.application;

import br.com.hulysses.hulysses_one.businesspartner.application.BusinessPartnerService;
import br.com.hulysses.hulysses_one.businesspartner.integration.dto.BusinessPartnerResponse;
import br.com.hulysses.hulysses_one.product.domain.Product;
import br.com.hulysses.hulysses_one.product.persistence.ProductRepository;
import br.com.hulysses.hulysses_one.product.presentation.dto.ProductRequest;
import br.com.hulysses.hulysses_one.product.presentation.dto.ProductResponse;
import br.com.hulysses.hulysses_one.shared.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;
    private final BusinessPartnerService businessPartnerService;

    public ProductService(ProductRepository repository, BusinessPartnerService businessPartnerService) {
        this.repository = repository;
        this.businessPartnerService = businessPartnerService;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        BusinessPartnerResponse supplier = businessPartnerService.findSupplierById(request.supplierId());
        Product product = new Product(request.name(), request.description(), request.price(), supplier.id());
        return ProductResponse.from(repository.save(product), supplier);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findById(id);
        BusinessPartnerResponse supplier = businessPartnerService.findSupplierById(request.supplierId());
        product.update(request.name(), request.description(), request.price(), supplier.id(),
                request.active() == null ? product.getActive() : request.active());
        return ProductResponse.from(repository.save(product), supplier);
    }

    public ProductResponse toResponse(Product product) {
        return ProductResponse.from(product, businessPartnerService.getById(product.getSupplierId()));
    }

    public Product findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Product", id));
    }

    public ProductResponse getById(Long id) {
        return toResponse(findById(id));
    }

    public List<ProductResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Long id) {
        findById(id);
        repository.deleteById(id);
    }

    public List<ProductResponse> findActive() {
        return repository.findByIsActiveTrue().stream().map(this::toResponse).toList();
    }

    public Optional<ProductResponse> findByName(String name) {
        return repository.findByNameIgnoreCase(name).map(this::toResponse);
    }

    public List<ProductResponse> findAllOrderByPrice() {
        return repository.findAllByOrderByPriceAsc().stream().map(this::toResponse).toList();
    }
}
