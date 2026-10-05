package br.com.hulysses.business_partner_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestDatabaseInitializer.class)
class BusinessPartnerApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void isolatedServiceSupportsCrudAndEligibilityWithoutPrincipalApplication() throws Exception {
        String body = """
                {"name":"Fornecedor","document":"12345678901","email":"supplier@example.com",
                 "phone":"11999999999","type":"INDIVIDUAL","roles":["SUPPLIER"]}
                """;
        long id = mapper.readTree(mvc.perform(post("/business-partners").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();
        mvc.perform(get("/business-partners/{id}/eligibility", id).param("role", "SUPPLIER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/business-partners/{id}/eligibility", id).param("role", "CUSTOMER"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/business-partners/{id}/eligibility", id).param("role", "INVALID"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/business-partners/999999/eligibility").param("role", "SUPPLIER"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/business-partners/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("Fornecedor", "Atualizado")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Atualizado"));
        mvc.perform(delete("/business-partners/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/business-partners/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void directRequestsValidateNestedContractAndSanitizeMalformedJson() throws Exception {
        mvc.perform(post("/business-partners").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.name").exists());
        mvc.perform(post("/business-partners").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Invalid request body or parameters"));
        mvc.perform(post("/business-partners").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Cliente","document":"123","email":"invalid","phone":"123","type":"INDIVIDUAL",
                 "roles":["CUSTOMER"],"addresses":[{"street":"Rua","number":"1","neighborhood":"Centro",
                 "city":"SP","state":"SP","country":"Brasil","postalCode":"123"}]}
                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.document").exists())
                .andExpect(jsonPath("$.fields.email").exists()).andExpect(jsonPath("$.fields['addresses[0].postalCode']").exists());
    }

    @Test
    void swaggerDocumentsHttpContract() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Business Partner Service API"))
                .andExpect(jsonPath("$.paths['/business-partners/{id}/eligibility'].get.parameters[1].name").value("role"))
                .andExpect(jsonPath("$.paths['/business-partners'].post.responses['201']").exists())
                .andExpect(jsonPath("$.components.schemas.BusinessPartnerRequest").exists())
                .andExpect(jsonPath("$.components.schemas.BusinessPartnerResponse").exists());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
