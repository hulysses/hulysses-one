package br.com.hulysses.hulysses_one;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import br.com.hulysses.hulysses_one.product.persistence.ProductRepository;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestDatabaseInitializer.class)
class UnavailableServiceTests {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) throws Exception {
        // The socket accepts no HTTP requests: calls time out, and another process cannot take the port.
        ServerSocket unavailable = new ServerSocket();
        unavailable.bind(new java.net.InetSocketAddress("localhost", 0));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { unavailable.close(); } catch (java.io.IOException ignored) { }
        }));
        registry.add("services.business-partner.url", () -> "http://localhost:" + unavailable.getLocalPort());
        registry.add("spring.cloud.openfeign.client.config.business-partner-service.connectTimeout", () -> 200);
        registry.add("spring.cloud.openfeign.client.config.business-partner-service.readTimeout", () -> 200);
    }

    @Test
    void principalStartsAloneAndRemoteOperationReturns503WithoutPersistingProduct() throws Exception {
        long count = products.count();
        String response = mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Produto","description":"Teste","price":10.00,"supplierId":1}
                """))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value("Business partner service is currently unavailable"))
                .andExpect(jsonPath("$.path").value("/products")).andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("localhost", "Feign", "ConnectException", "stackTrace", "http://");
        assertThat(products.count()).isEqualTo(count);
        mvc.perform(get("/business-partners")).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/sales-orders/total")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
