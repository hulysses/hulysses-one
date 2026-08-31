package br.com.hulysses.hulysses_one.product.application;

import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartner;
import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartnerRole;
import br.com.hulysses.hulysses_one.businesspartner.persistence.BusinessPartnerRepository;
import br.com.hulysses.hulysses_one.product.domain.Product;
import br.com.hulysses.hulysses_one.product.persistence.ProductRepository;
import br.com.hulysses.hulysses_one.product.presentation.dto.ProductRequest;
import br.com.hulysses.hulysses_one.shared.domain.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final BusinessPartnerRepository businessPartnerRepository;

    public ProductService(
            ProductRepository repository,
            BusinessPartnerRepository businessPartnerRepository
    ) {
        this.repository = repository;
        this.businessPartnerRepository = businessPartnerRepository;
    }

    public Product create(
            ProductRequest request
    ) {

        BusinessPartner supplier =
                businessPartnerRepository
                        .findById(request.supplierId())
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Supplier",
                                        request.supplierId()
                                )
                        );

        if (!supplier.getRoles().contains(
                BusinessPartnerRole.SUPPLIER
        )) {
            throw new IllegalArgumentException(
                    "Business partner is not a supplier"
            );
        }

        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                supplier
        );

        return repository.save(product);
    }

    public Product update(
            Long id,
            ProductRequest request
    ) {

        Product product = findById(id);

        BusinessPartner supplier =
                businessPartnerRepository
                        .findById(request.supplierId())
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Supplier",
                                        request.supplierId()
                                )
                        );

        if (!supplier.getRoles().contains(
                BusinessPartnerRole.SUPPLIER
        )) {
            throw new IllegalArgumentException(
                    "Business partner is not a supplier"
            );
        }

        product.update(
                request.name(),
                request.description(),
                request.price(),
                supplier,
                request.active() == null
                        ? product.getActive()
                        : request.active()
        );

        return repository.save(product);
    }

    public Product findById(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Product",
                                id
                        )
                );
    }

    public List<Product> findAll() {
        return repository.findAll();
    }

    public void delete(Long id) {
        findById(id);
        repository.deleteById(id);
    }

    public List<Product> findActive() {
        return repository.findByIsActiveTrue();
    }

    public Optional<Product> findByName(
            String name
    ) {
        return repository.findByNameIgnoreCase(name);
    }

    public List<Product> findAllOrderByPrice() {
        return repository.findAllByOrderByPriceAsc();
    }
}