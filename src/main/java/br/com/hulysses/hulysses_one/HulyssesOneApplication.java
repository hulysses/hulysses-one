package br.com.hulysses.hulysses_one;

import br.com.hulysses.hulysses_one.businesspartner.application.BusinessPartnerService;
import br.com.hulysses.hulysses_one.businesspartner.domain.Address;
import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartner;
import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartnerRole;
import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartnerType;
import br.com.hulysses.hulysses_one.product.application.ProductService;
import br.com.hulysses.hulysses_one.product.domain.Product;
import br.com.hulysses.hulysses_one.sales.application.SalesOrderService;
import br.com.hulysses.hulysses_one.sales.domain.SalesOrder;
import br.com.hulysses.hulysses_one.sales.domain.SalesOrderProduct;
import br.com.hulysses.hulysses_one.shared.domain.EntityNotFoundException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDateTime;

@SpringBootApplication
public class HulyssesOneApplication {

    public static void main(String[] args) {
        SpringApplication.run(HulyssesOneApplication.class, args);

        BusinessPartnerService partnerService = new BusinessPartnerService();
        ProductService productService = new ProductService();
        SalesOrderService orderService = new SalesOrderService();

        BusinessPartner supplier = new BusinessPartner(
                1L,
                "Hulysses Tecnologia Ltda",
                "12345678000199",
                "contato@hulysses.com",
                "45999999999",
                BusinessPartnerType.COMPANY
        );

        supplier.addRole(
                BusinessPartnerRole.SUPPLIER
        );
        supplier.addAddress(
                new Address(
                        1L,
                        "Avenida Brasil",
                        "1000",
                        null,
                        "Centro",
                        "Cascavel",
                        "PR",
                        "Brasil",
                        "85801000"
                )
        );

        BusinessPartner customer =
                new BusinessPartner(
                        2L,
                        "Joao da Silva",
                        "12345678901",
                        "joao@email.com",
                        "45988888888",
                        BusinessPartnerType.INDIVIDUAL
                );

        customer.addRole(
                BusinessPartnerRole.CUSTOMER
        );
        customer.addAddress(
                new Address(
                        2L,
                        "Rua Parana",
                        "500",
                        "Apartamento 10",
                        "Centro",
                        "Cascavel",
                        "PR",
                        "Brasil",
                        "85810000"
                )
        );

        BusinessPartner secondCustomer =
                new BusinessPartner(
                        3L,
                        "Maria Oliveira",
                        "98765432100",
                        "maria@email.com",
                        "45977777777",
                        BusinessPartnerType.INDIVIDUAL
                );

        secondCustomer.addRole(
                BusinessPartnerRole.CUSTOMER
        );

        partnerService.create(supplier);
        partnerService.create(customer);
        partnerService.create(secondCustomer);

        Product notebook =
                new Product(
                        1L,
                        "Notebook",
                        "Notebook para uso empresarial",
                        4500.00,
                        supplier
                );

        Product monitor =
                new Product(
                        2L,
                        "Monitor",
                        "Monitor 27 polegadas",
                        1500.00,
                        supplier
                );

        Product keyboard =
                new Product(
                        3L,
                        "Teclado",
                        "Teclado mecanico",
                        350.00,
                        supplier
                );


        productService.create(notebook);
        productService.create(monitor);
        productService.create(keyboard);

        SalesOrder order =
                new SalesOrder(
                        1L,
                        "PED-0001",
                        LocalDateTime.now(),
                        "OPEN",
                        customer
                );

        SalesOrderProduct notebookItem =
                new SalesOrderProduct(
                        1L,
                        order,
                        notebook,
                        2
                );

        SalesOrderProduct monitorItem =
                new SalesOrderProduct(
                        2L,
                        order,
                        monitor,
                        1
                );


        order.addProduct(notebookItem);
        order.addProduct(monitorItem);
        orderService.create(order);

        SalesOrder secondOrder =
                new SalesOrder(
                        2L,
                        "PED-0002",
                        LocalDateTime.now(),
                        "OPEN",
                        secondCustomer
                );

        SalesOrderProduct keyboardItem =
                new SalesOrderProduct(
                        3L,
                        secondOrder,
                        keyboard,
                        2
                );

        secondOrder.addProduct(keyboardItem);
        orderService.create(secondOrder);

        System.out.println(
                "Cliente filtrado por ID: "
                        + partnerService
                        .findById(2L)
                        .getName()
        );

        System.out.println(
                "Produtos cadastrados: "
                        + productService.findAll().size()
        );

        System.out.println(
                "Pedidos cadastrados: "
                        + orderService.findAll().size()
        );

        partnerService
                .findCustomers()
                .forEach(partner ->
                        System.out.println(
                                "Cliente: "
                                        + partner.getName()
                        )
                );

        productService
                .findAllOrderByPrice()
                .forEach(product ->
                        System.out.println(
                                product.getName()
                                        + " - R$ "
                                        + product.getPrice()
                        )
                );

        productService
                .findByName("Monitor")
                .ifPresent(product ->
                        System.out.println(
                                "Produto filtrado: "
                                        + product.getName()
                        )
                );

        System.out.println(
                "\n5 - TRANSFORMACAO DE COLECAO"
        );

        productService
                .findProductNames()
                .forEach(name ->
                        System.out.println(
                                "Produto: " + name
                        )
                );

        System.out.println(
                "\n6 - PEDIDOS POR CLIENTE"
        );

        orderService
                .findByCustomer(customer.getId())
                .forEach(foundOrder ->
                        System.out.println(
                                foundOrder.getOrderNumber()
                        )
                );

        System.out.println(
                "Total vendido: R$ "
                        + orderService.calculateTotalSales()
        );

        System.out.println(
                "\n8 - EXCLUSAO"
        );

        productService.delete(3L);

        System.out.println(
                "Produtos apos exclusao: "
                        + productService.findAll().size()
        );

        try {
            productService.findById(999L);
        } catch (EntityNotFoundException exception) {
            System.out.println(
                    "Erro tratado: "
                            + exception.getMessage()
            );
        }
    }
}