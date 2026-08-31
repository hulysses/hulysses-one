package br.com.hulysses.hulysses_one.sales.application;

import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartner;
import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartnerRole;
import br.com.hulysses.hulysses_one.businesspartner.persistence.BusinessPartnerRepository;
import br.com.hulysses.hulysses_one.product.domain.Product;
import br.com.hulysses.hulysses_one.product.persistence.ProductRepository;
import br.com.hulysses.hulysses_one.sales.domain.SalesOrder;
import br.com.hulysses.hulysses_one.sales.domain.SalesOrderProduct;
import br.com.hulysses.hulysses_one.sales.persistence.SalesOrderRepository;
import br.com.hulysses.hulysses_one.sales.presentation.dto.SalesOrderItemRequest;
import br.com.hulysses.hulysses_one.sales.presentation.dto.SalesOrderRequest;
import br.com.hulysses.hulysses_one.shared.domain.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SalesOrderService {

    private final SalesOrderRepository repository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final ProductRepository productRepository;

    public SalesOrderService(
            SalesOrderRepository repository,
            BusinessPartnerRepository businessPartnerRepository,
            ProductRepository productRepository
    ) {
        this.repository = repository;
        this.businessPartnerRepository =
                businessPartnerRepository;
        this.productRepository =
                productRepository;
    }

    public SalesOrder create(
            SalesOrderRequest request
    ) {

        BusinessPartner customer =
                businessPartnerRepository
                        .findById(request.customerId())
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Customer",
                                        request.customerId()
                                )
                        );

        if (repository.existsByOrderNumber(
                request.orderNumber()
        )) {
            throw new IllegalArgumentException(
                    "Order number already registered"
            );
        }

        if (!customer.getRoles().contains(
                BusinessPartnerRole.CUSTOMER
        )) {
            throw new IllegalArgumentException(
                    "Business partner is not a customer"
            );
        }

        SalesOrder order =
                new SalesOrder(
                        request.orderNumber(),
                        request.orderDate(),
                        request.status(),
                        customer
                );

        for (SalesOrderItemRequest itemRequest :
                request.items()) {

            Product product =
                    productRepository
                            .findById(itemRequest.productId())
                            .orElseThrow(() ->
                                    new EntityNotFoundException(
                                            "Product",
                                            itemRequest.productId()
                                    )
                            );

            SalesOrderProduct item =
                    new SalesOrderProduct(
                            order,
                            product,
                            itemRequest.quantity()
                    );

            order.addProduct(item);
        }

        return repository.save(order);
    }

    public SalesOrder update(
            Long id,
            SalesOrderRequest request
    ) {
        SalesOrder order = findById(id);

        BusinessPartner customer =
                businessPartnerRepository
                        .findById(request.customerId())
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Customer",
                                        request.customerId()
                                )
                        );

        if (repository.existsByOrderNumberAndIdNot(
                request.orderNumber(),
                id
        )) {
            throw new IllegalArgumentException(
                    "Order number already registered"
            );
        }

        if (!customer.getRoles().contains(
                BusinessPartnerRole.CUSTOMER
        )) {
            throw new IllegalArgumentException(
                    "Business partner is not a customer"
            );
        }

        order.update(
                request.orderNumber(),
                request.orderDate(),
                request.status(),
                customer
        );

        order.clearProducts();

        for (SalesOrderItemRequest itemRequest :
                request.items()) {

            Product product =
                    productRepository
                            .findById(itemRequest.productId())
                            .orElseThrow(() ->
                                    new EntityNotFoundException(
                                            "Product",
                                            itemRequest.productId()
                                    )
                            );

            SalesOrderProduct item =
                    new SalesOrderProduct(
                            order,
                            product,
                            itemRequest.quantity()
                    );

            order.addProduct(item);
        }

        return repository.save(order);
    }

    public SalesOrder findById(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Sales order",
                                id
                        )
                );
    }

    public List<SalesOrder> findAll() {
        return repository.findAll();
    }

    public void delete(Long id) {
        findById(id);

        repository.deleteById(id);
    }

    public List<SalesOrder> findByCustomer(
            Long customerId
    ) {
        return repository.findByCustomerId(
                customerId
        );
    }

    public List<SalesOrder> findByStatus(
            String status
    ) {
        return repository.findByStatusIgnoreCase(
                status
        );
    }

    public BigDecimal calculateTotalSales() {
        return repository.calculateTotalSales();
    }
}