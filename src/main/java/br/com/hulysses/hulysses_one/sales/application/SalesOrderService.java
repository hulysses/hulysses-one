package br.com.hulysses.hulysses_one.sales.application;

import br.com.hulysses.hulysses_one.sales.domain.SalesOrder;
import br.com.hulysses.hulysses_one.shared.domain.DuplicateEntityException;
import br.com.hulysses.hulysses_one.shared.domain.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SalesOrderService {

    private final Map<Long, SalesOrder> orders = new HashMap<>();

    public SalesOrder create(SalesOrder order) {
        validateOrder(order);

        if (orders.containsKey(order.getId())) {
            throw new DuplicateEntityException(
                    "Sales order",
                    order.getId()
            );
        }

        orders.put(order.getId(), order);

        return order;
    }

    public SalesOrder update(SalesOrder order) {
        validateOrder(order);
        findById(order.getId());

        orders.put(order.getId(), order);

        return order;
    }

    public void delete(Long id) {
        findById(id);

        orders.remove(id);
    }

    public SalesOrder findById(Long id) {
        validateId(id);

        SalesOrder order = orders.get(id);

        if (order == null) {
            throw new EntityNotFoundException(
                    "Sales order",
                    id
            );
        }

        return order;
    }

    public List<SalesOrder> findAll() {
        return new ArrayList<>(orders.values());
    }

    public List<SalesOrder> findByCustomer(Long customerId) {
        validateId(customerId);

        return orders.values()
                .stream()
                .filter(order ->
                        order.getCustomer()
                                .getId()
                                .equals(customerId)
                )
                .toList();
    }

    public List<SalesOrder> findByStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException(
                    "Status is required"
            );
        }

        return orders.values()
                .stream()
                .filter(order ->
                        order.getStatus()
                                .equalsIgnoreCase(status.trim())
                )
                .toList();
    }

    public List<SalesOrder> findAllOrderByTotalDescending() {
        return orders.values()
                .stream()
                .sorted(
                        Comparator.comparing(
                                order ->
                                        order.getTotalAmount() == null
                                                ? 0.0
                                                : order.getTotalAmount(),
                                Comparator.reverseOrder()
                        )
                )
                .toList();
    }

    public double calculateTotalSales() {
        return orders.values()
                .stream()
                .map(SalesOrder::getTotalAmount)
                .filter(total -> total != null)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private void validateOrder(SalesOrder order) {
        if (order == null) {
            throw new IllegalArgumentException(
                    "Sales order is required"
            );
        }

        validateId(order.getId());
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Id must be greater than zero"
            );
        }
    }
}