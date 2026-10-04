package br.com.hulysses.hulysses_one;

import br.com.hulysses.hulysses_one.businesspartner.application.BusinessPartnerService;
import br.com.hulysses.hulysses_one.businesspartner.presentation.BusinessPartnerController;
import br.com.hulysses.hulysses_one.product.application.ProductService;
import br.com.hulysses.hulysses_one.product.presentation.ProductController;
import br.com.hulysses.hulysses_one.sales.application.SalesOrderService;
import br.com.hulysses.hulysses_one.sales.presentation.SalesOrderController;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureTests {
    @Test
    void controllersDependOnlyOnTheirApplicationServices() {
        for (Class<?> controller : List.of(BusinessPartnerController.class, ProductController.class, SalesOrderController.class)) {
            assertThat(Arrays.stream(controller.getDeclaredFields()).map(field -> field.getType().getPackageName()))
                    .allMatch(packageName -> packageName.equals(controller.getPackageName().replace(".presentation", ".application")));
        }
    }

    @Test
    void servicesAccessOnlyRepositoriesOwnedByTheirFeature() {
        for (Class<?> service : List.of(BusinessPartnerService.class, ProductService.class, SalesOrderService.class)) {
            var repositories = Arrays.stream(service.getDeclaredFields()).map(field -> field.getType())
                    .filter(Repository.class::isAssignableFrom).toList();
            assertThat(repositories).hasSize(1).allMatch(repository -> repository.getPackageName()
                    .equals(service.getPackageName().replace(".application", ".persistence")));
        }
    }
}
