package br.com.hulysses.hulysses_one.shared.exception;

import br.com.hulysses.hulysses_one.product.domain.exception.ProductException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTests {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sales-orders");

    @ParameterizedTest
    @MethodSource("domainExceptions")
    void existingDomainExceptionsHaveConsistentSafeResponses(DomainException exception) {
        var response = handler.handleDomain(exception, request);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo(exception.getMessage());
        assertThat(response.getBody().path()).isEqualTo("/sales-orders");
        assertThat(response.getBody().fields()).isEmpty();
    }

    static Stream<DomainException> domainExceptions() {
        return Stream.of(new ProductException("Product is inactive and cannot be added to the order"),
                new DuplicateEntityException("Order number already registered"));
    }

    @Test
    void unexpectedErrorsNeverReturnInternalDetails() {
        var response = handler.handleUnexpected(new IllegalStateException("internal implementation detail"), request);
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().toString()).doesNotContain("internal implementation detail", "IllegalStateException");
    }

    @Test
    void unavailableServiceUsesExistingSafeErrorContract() {
        var response = handler.handleUnavailable(new ExternalServiceUnavailableException(
                new IllegalStateException("http://internal-host:8081 connection refused")), request);
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().message()).isEqualTo("Business partner service is currently unavailable");
        assertThat(response.getBody().path()).isEqualTo("/sales-orders");
        assertThat(response.getBody().toString()).doesNotContain("internal-host", "connection refused", "IllegalStateException");
    }
}
