package br.com.hulysses.hulysses_one.sales.persistence;

import br.com.hulysses.hulysses_one.sales.domain.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface SalesOrderRepository
        extends JpaRepository<SalesOrder, Long> {

    List<SalesOrder> findByCustomerId(
            Long customerId
    );

    List<SalesOrder> findByStatusIgnoreCase(
            String status
    );

    @Query("""
                select coalesce(sum(o.totalAmount), 0)
                from SalesOrder o
            """)
    BigDecimal calculateTotalSales();

    boolean existsByOrderNumber(String orderNumber);

    boolean existsByOrderNumberAndIdNot(
            String orderNumber,
            Long id
    );
}