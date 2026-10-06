package br.com.hulysses.config_server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ConfigServerTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @ParameterizedTest
    @CsvSource({
            "hulysses-app,dev,localhost:5432/hulysses_one",
            "hulysses-app,prod,hulysses-db:5432/hulysses",
            "business-partner-service,dev,localhost:5432/business_partner",
            "business-partner-service,prod,business-partner-db:5432/business_partner"
    })
    void nativeRepositoryServesEachApplicationAndProfileWithoutSecrets(String application, String profile,
                                                                     String database) throws Exception {
        String body = mvc.perform(get("/{application}/{profile}", application, profile))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value(application))
                .andExpect(jsonPath("$.profiles[0]").value(profile)).andReturn().getResponse().getContentAsString();
        Map<String, JsonNode> properties = new LinkedHashMap<>();
        // Highest-priority source comes first, as in the Config Client.
        for (JsonNode source : mapper.readTree(body).path("propertySources")) {
            source.path("source").properties().forEach(entry -> properties.putIfAbsent(entry.getKey(), entry.getValue()));
        }
        assertThat(properties.get("spring.datasource.url").asText()).contains(database);
        assertThat(properties.get("spring.jpa.open-in-view").asBoolean()).isFalse();
        assertThat(properties.get("spring.jpa.hibernate.ddl-auto").asText()).contains("JPA_DDL_AUTO");
        assertThat(properties).doesNotContainKeys("spring.datasource.password", "spring.rabbitmq.password");
        assertThat(body).doesNotContain("DB_PASSWORD", "PARTNER_DB_PASSWORD", "RABBITMQ_PASSWORD");
        if (application.equals("business-partner-service")) {
            assertThat(properties.get("spring.rabbitmq.host").asText())
                    .contains(profile.equals("prod") ? "rabbitmq" : "localhost");
            assertThat(properties.get("spring.batch.job.enabled").asBoolean()).isFalse();
            assertThat(properties.get("spring.batch.jdbc.initialize-schema").asText()).contains("always");
        }
        if (application.equals("hulysses-app")) {
            assertThat(properties.get("services.business-partner.url").asText())
                    .contains(profile.equals("prod") ? "business-partner-service:8081" : "localhost:8081");
            assertThat(properties.get("spring.cloud.openfeign.client.config.business-partner-service.readTimeout").asText())
                    .contains("FEIGN_READ_TIMEOUT");
        }
    }

    @Test
    void exposesOnlyHealthAndNoAdministrativeActuatorEndpoints() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        // /actuator/env is not registered; the Config Server catch-all route can instead return a config Environment.
        // The exposure configuration itself must contain only health.
        assertThat(mvc.perform(get("/actuator")).andReturn().getResponse().getContentAsString())
                .doesNotContain("\"env\"", "\"configprops\"", "\"shutdown\"", "\"refresh\"");
    }
}
