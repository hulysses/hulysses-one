package br.com.hulysses.hulysses_one;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RemoteFailureTests {
    private static HttpServer remote;
    private static volatile int status;
    private static volatile String body;
    private static final AtomicInteger calls = new AtomicInteger();
    @Autowired MockMvc mvc;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) throws Exception {
        remote = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        remote.createContext("/business-partners", exchange -> {
            calls.incrementAndGet();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        remote.start();
        registry.add("services.business-partner.url", () -> "http://localhost:" + remote.getAddress().getPort());
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:remote_failure_test;MODE=PostgreSQL");
    }

    @AfterAll
    static void stop() { if (remote != null) remote.stop(0); }

    @Test
    void upstream5xxAndMalformedResponsesAre503AndAreNeverRetried() throws Exception {
        for (int code : new int[] {500, 503, 404, 200}) {
            status = code;
            body = "internal-host connection failed: SECRET";
            calls.set(0);
            String response = mvc.perform(get("/business-partners/1"))
                    .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.path").value("/business-partners/1"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(response).doesNotContain("SECRET", "internal-host", "Feign", "connection failed");
            assertThat(calls).hasValue(1);
        }
    }

    @Test
    void recognizedBusinessRejectionsKeepTheirStatusWithoutForwardingTechnicalMessages() throws Exception {
        for (int code : new int[] {400, 404, 409}) {
            status = code;
            body = """
                    {"timestamp":"2026-10-04T10:00:00","status":%d,"error":"Error",
                     "message":"internal-host SECRET","path":"/internal","fields":{}}
                    """.formatted(code);
            mvc.perform(get("/business-partners/1")).andExpect(status().is(code))
                    .andExpect(jsonPath("$.path").value("/business-partners/1"))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SECRET"))));
        }
    }
}
