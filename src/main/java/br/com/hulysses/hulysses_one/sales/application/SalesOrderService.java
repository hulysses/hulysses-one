package br.com.hulysses.hulysses_one.sales.application;

import br.com.hulysses.hulysses_one.businesspartner.application.BusinessPartnerService;
import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartner;
import br.com.hulysses.hulysses_one.product.application.ProductService;
import br.com.hulysses.hulysses_one.product.domain.Product;
import br.com.hulysses.hulysses_one.product.domain.exception.ProductException;
import br.com.hulysses.hulysses_one.sales.domain.SalesOrder;
import br.com.hulysses.hulysses_one.sales.domain.SalesOrderProduct;
import br.com.hulysses.hulysses_one.sales.persistence.SalesOrderRepository;
import br.com.hulysses.hulysses_one.sales.presentation.dto.SalesOrderItemRequest;
import br.com.hulysses.hulysses_one.sales.presentation.dto.SalesOrderRequest;
import br.com.hulysses.hulysses_one.sales.presentation.dto.SalesOrderResponse;
import br.com.hulysses.hulysses_one.shared.exception.DuplicateEntityException;
import br.com.hulysses.hulysses_one.shared.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SalesOrderService {

    private final SalesOrderRepository repository;
    private final BusinessPartnerService businessPartnerService;
    private final ProductService productService;

    public SalesOrderService(SalesOrderRepository repository, BusinessPartnerService businessPartnerService,
                             ProductService productService) {
        this.repository = repository;
        this.businessPartnerService = businessPartnerService;
        this.productService = productService;
    }

    @Transactional
    public SalesOrderResponse create(SalesOrderRequest request) {
        BusinessPartner customer = businessPartnerService.findCustomerById(request.customerId());
        if (repository.existsByOrderNumber(request.orderNumber())) {
            throw new DuplicateEntityException("Order number already registered");
        }
        SalesOrder order = new SalesOrder(request.orderNumber(), request.orderDate(), request.status(), customer);
        addItems(order, request.items());
        return SalesOrderResponse.from(repository.save(order));
    }

    @Transactional
    public SalesOrderResponse update(Long id, SalesOrderRequest request) {
        SalesOrder order = findById(id);
        BusinessPartner customer = businessPartnerService.findCustomerById(request.customerId());
        if (repository.existsByOrderNumberAndIdNot(request.orderNumber(), id)) {
            throw new DuplicateEntityException("Order number already registered");
        }
        order.update(request.orderNumber(), request.orderDate(), request.status(), customer);
        order.clearProducts();
        addItems(order, request.items());
        return SalesOrderResponse.from(repository.save(order));
    }

    private void addItems(SalesOrder order, List<SalesOrderItemRequest> items) {
        for (SalesOrderItemRequest item : items) {
            Product product = productService.findById(item.productId());
            if (!product.getActive()) {
                throw new ProductException("Product is inactive and cannot be added to the order");
            }
            order.addProduct(new SalesOrderProduct(order, product, item.quantity()));
        }
    }

    private SalesOrder findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Sales order", id));
    }

    public SalesOrderResponse getById(Long id) {
        return SalesOrderResponse.from(findById(id));
    }

    public List<SalesOrderResponse> findAll() {
        return repository.findAll().stream().map(SalesOrderResponse::from).toList();
    }

    @Transactional
    public void delete(Long id) {
        findById(id);
        repository.deleteById(id);
    }

    public List<SalesOrderResponse> findByCustomer(Long customerId) {
        return repository.findByCustomerId(customerId).stream().map(SalesOrderResponse::from).toList();
    }

    public List<SalesOrderResponse> findByStatus(String status) {
        return repository.findByStatusIgnoreCase(status).stream().map(SalesOrderResponse::from).toList();
    }

    public BigDecimal calculateTotalSales() {
        return repository.calculateTotalSales();
    }
}
