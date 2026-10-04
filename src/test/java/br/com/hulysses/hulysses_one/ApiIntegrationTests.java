package br.com.hulysses.hulysses_one;

import br.com.hulysses.hulysses_one.businesspartner.persistence.BusinessPartnerRepository;
import br.com.hulysses.hulysses_one.product.persistence.ProductRepository;
import br.com.hulysses.hulysses_one.sales.persistence.SalesOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestDatabaseInitializer.class)
class ApiIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired BusinessPartnerRepository partners;
    @Autowired ProductRepository products;
    @Autowired SalesOrderRepository orders;
    @Autowired DataSource dataSource;

    @BeforeEach
    void clearIsolatedDatabase() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:")
                    || connection.getSchema().startsWith("etapa1_test_")).isTrue();
        }
        orders.deleteAll();
        products.deleteAll();
        partners.deleteAll();
    }

    @Test
    void partnerCrudPreservesRolesAddressesAndActiveDefaults() throws Exception {
        String request = partner("Maria", "12345678901", "CUSTOMER");
        JsonNode created = json(mvc.perform(post("/business-partners").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.addresses", hasSize(1))).andReturn());
        long id = created.path("id").asLong();
        mvc.perform(get("/business-partners/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value("12345678901"));
        String updated = request.replace("Maria", "Maria Silva").replace("CUSTOMER", "SUPPLIER")
                .replace("\"postalCode\":\"01001000\"", "\"postalCode\":\"02002000\"");
        mvc.perform(put("/business-partners/{id}", id).contentType(MediaType.APPLICATION_JSON).content(updated))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Maria Silva"))
                .andExpect(jsonPath("$.active").value(false)).andExpect(jsonPath("$.roles[0]").value("SUPPLIER"))
                .andExpect(jsonPath("$.addresses[0].postalCode").value("02002000"));
        mvc.perform(put("/business-partners/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(updated.replace(",\"active\":false", "")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(delete("/business-partners/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/business-partners/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void partnerQueriesUsePartialNameIgnoringCaseAndActualRoles() throws Exception {
        createPartner("Maria Cliente", "12345678901", "CUSTOMER");
        createPartner("Maria Fornecedor", "12345678902", "SUPPLIER");
        createPartner("Joao", "12345678903", "USER");
        mvc.perform(get("/business-partners")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/business-partners/search").param("name", "mArIa"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/business-partners/customers")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].name").value("Maria Cliente"));
        mvc.perform(get("/business-partners/suppliers")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].name").value("Maria Fornecedor"));
        mvc.perform(get("/business-partners/search").param("name", "inexistente"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void productCrudAndQueriesPreserveSupplierAndSortByPrice() throws Exception {
        long supplier = createPartner("Fornecedor", "12345678901", "SUPPLIER");
        long cheap = createProduct("Barato", "10.00", supplier);
        long expensive = createProduct("Caro", "25.50", supplier);
        mvc.perform(get("/products/{id}", cheap)).andExpect(status().isOk())
                .andExpect(jsonPath("$.supplier.id").value(supplier))
                .andExpect(jsonPath("$.supplier.addresses[0].postalCode").value("01001000"));
        mvc.perform(put("/products/{id}", expensive).contentType(MediaType.APPLICATION_JSON)
                        .content(product("Caro", "25.50", supplier)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(put("/products/{id}", expensive).contentType(MediaType.APPLICATION_JSON)
                        .content(product("Caro", "25.50", supplier).replace(",\"active\":false", "")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/products/active")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(cheap));
        mvc.perform(get("/products/ordered-by-price")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(cheap)).andExpect(jsonPath("$[1].id").value(expensive));
        mvc.perform(get("/products")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(delete("/products/{id}", expensive)).andExpect(status().isNoContent());
        mvc.perform(get("/products/{id}", expensive)).andExpect(status().isNotFound());
    }

    @Test
    void salesCrudQueriesAndTotalsUseExistingCalculation() throws Exception {
        long supplier = createPartner("Fornecedor", "12345678901", "SUPPLIER");
        long customer = createPartner("Cliente", "12345678902", "CUSTOMER");
        long product = createProduct("Produto", "10.50", supplier);
        mvc.perform(get("/sales-orders/total")).andExpect(status().isOk()).andExpect(jsonPath("$").value(0));
        String request = order("VENDA-001", customer, product, 2);
        long id = json(mvc.perform(post("/sales-orders").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.totalAmount").value(21.00))
                .andExpect(jsonPath("$.products[0].product.supplier.id").value(supplier)).andReturn()).path("id").asLong();
        mvc.perform(get("/sales-orders/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.id").value(customer));
        mvc.perform(get("/sales-orders")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/sales-orders/customer/{customer}", customer)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/sales-orders/status/open")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/sales-orders/customer/999999")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/sales-orders/status/unknown")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(put("/sales-orders/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(order("VENDA-001", customer, product, 3).replace("OPEN", "CUSTOM-STATUS")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.products", hasSize(1)))
                .andExpect(jsonPath("$.totalAmount").value(31.50));
        mvc.perform(get("/sales-orders/total")).andExpect(status().isOk()).andExpect(content().string("31.50"));
        mvc.perform(delete("/sales-orders/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/sales-orders/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/sales-orders/total")).andExpect(status().isOk()).andExpect(jsonPath("$").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/business-partners", "/products", "/sales-orders"})
    void missingRecordsReturn404ForReadUpdateAndDelete(String resource) throws Exception {
        String request = switch (resource) {
            case "/business-partners" -> partner("Cliente", "12345678901", "CUSTOMER");
            case "/products" -> product("Produto", "1.00", 1);
            default -> order("VENDA-001", 1, 1, 1);
        };
        String path = resource + "/999999";
        expectError(get(path), 404, path);
        expectError(put(path).contentType(MediaType.APPLICATION_JSON).content(request), 404, path);
        expectError(delete(path), 404, path);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/business-partners", "/products", "/sales-orders"})
    void emptyBodiesAreValidatedForCreateAndUpdate(String resource) throws Exception {
        expectError(post(resource).contentType(MediaType.APPLICATION_JSON).content("{}"), 400, resource);
        expectError(put(resource + "/1").contentType(MediaType.APPLICATION_JSON).content("{}"), 400, resource + "/1");
    }

    @Test
    void partnerValidationRejectsMalformedDocumentsEmailPostalCodeAndNullElements() throws Exception {
        String valid = partner("Cliente", "12345678901", "CUSTOMER");
        List<String> invalid = List.of(valid.replace("12345678901", "123"), valid.replace("INDIVIDUAL", "COMPANY"),
                valid.replace("cliente@example.com", "invalid-email"), valid.replace("01001000", "01001-000"),
                valid.replace("[\"CUSTOMER\"]", "[null]"), valid.replace("\"name\":\"Cliente\"", "\"name\":\" \""),
                valid.replace("\"name\":\"Cliente\"", "\"name\":\"" + "a".repeat(256) + "\""),
                valid.replace(address(), "null"));
        for (String request : invalid) {
            expectError(post("/business-partners").contentType(MediaType.APPLICATION_JSON).content(request), 400, "/business-partners");
        }
        mvc.perform(post("/business-partners").contentType(MediaType.APPLICATION_JSON).content(valid.replace("12345678901", "123")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.document").exists());
        mvc.perform(post("/business-partners").contentType(MediaType.APPLICATION_JSON).content(valid.replace("01001000", "123")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields['addresses[0].postalCode']").exists());
        createPartner("Empresa", "12345678901234", "SUPPLIER");
        assertThat(partners.count()).isEqualTo(1);
    }

    @Test
    void productValidationMatchesPriceAndPersistenceConstraints() throws Exception {
        String valid = product("Produto", "10.00", 1);
        for (String request : List.of(valid.replace("10.00", "0"), valid.replace("10.00", "-1"),
                valid.replace("10.00", "10.001"), valid.replace("10.00", "10000000000000"),
                valid.replace("\"supplierId\":1", "\"supplierId\":0"))) {
            expectError(post("/products").contentType(MediaType.APPLICATION_JSON).content(request), 400, "/products");
        }
        assertThat(products.count()).isZero();
    }

    @Test
    void salesValidationRejectsInvalidQuantityIdsAndNullItems() throws Exception {
        String valid = order("VENDA-001", 1, 1, 1);
        for (String request : List.of(valid.replace("\"quantity\":1", "\"quantity\":0"),
                valid.replace("\"quantity\":1", "\"quantity\":null"),
                valid.replace("\"productId\":1", "\"productId\":0"),
                valid.replace("\"customerId\":1", "\"customerId\":0"),
                valid.replace("{\"productId\":1,\"quantity\":1}", "null"),
                valid.replace("[{\"productId\":1,\"quantity\":1}]", "[]"))) {
            expectError(post("/sales-orders").contentType(MediaType.APPLICATION_JSON).content(request), 400, "/sales-orders");
        }
        assertThat(orders.count()).isZero();
    }

    @Test
    void duplicateDocumentsAndOrderNumbersAreRejectedOnCreateAndUpdate() throws Exception {
        long supplier = createPartner("Fornecedor", "12345678901", "SUPPLIER");
        long customer = createPartner("Cliente", "12345678902", "CUSTOMER");
        expectError(post("/business-partners").contentType(MediaType.APPLICATION_JSON)
                .content(partner("Duplicado", "12345678901", "CUSTOMER")), 400, "/business-partners");
        expectError(put("/business-partners/" + customer).contentType(MediaType.APPLICATION_JSON)
                .content(partner("Duplicado", "12345678901", "CUSTOMER")), 400, "/business-partners/" + customer);
        long product = createProduct("Produto", "10.00", supplier);
        createOrder("VENDA-001", customer, product);
        long other = createOrder("VENDA-002", customer, product);
        expectError(post("/sales-orders").contentType(MediaType.APPLICATION_JSON)
                .content(order("VENDA-001", customer, product, 1)), 400, "/sales-orders");
        expectError(put("/sales-orders/" + other).contentType(MediaType.APPLICATION_JSON)
                .content(order("VENDA-001", customer, product, 1)), 400, "/sales-orders/" + other);
        mvc.perform(get("/sales-orders/{id}", other)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("VENDA-002"));
    }

    @Test
    void supplierCustomerAndReferencedProductMustExistAndHaveRequiredRoles() throws Exception {
        long user = createPartner("Usuario", "12345678901", "USER");
        long customer = createPartner("Cliente", "12345678902", "CUSTOMER");
        expectError(post("/products").contentType(MediaType.APPLICATION_JSON).content(product("Produto", "1.00", user)), 400, "/products");
        expectError(post("/products").contentType(MediaType.APPLICATION_JSON).content(product("Produto", "1.00", 999999)), 404, "/products");
        expectError(post("/sales-orders").contentType(MediaType.APPLICATION_JSON).content(order("VENDA-001", user, 1, 1)), 400, "/sales-orders");
        expectError(post("/sales-orders").contentType(MediaType.APPLICATION_JSON).content(order("VENDA-001", 999999, 1, 1)), 404, "/sales-orders");
        expectError(post("/sales-orders").contentType(MediaType.APPLICATION_JSON).content(order("VENDA-001", customer, 999999, 1)), 404, "/sales-orders");
    }

    @Test
    void failedSalesUpdateRollsBackChangedFieldsAndRemovedItems() throws Exception {
        long supplier = createPartner("Fornecedor", "12345678901", "SUPPLIER");
        long customer = createPartner("Cliente", "12345678902", "CUSTOMER");
        long product = createProduct("Produto", "10.00", supplier);
        long order = createOrder("VENDA-001", customer, product);
        String update = order("VENDA-ALTERADA", customer, product, 5)
                .replace("OPEN", "CHANGED").replace("\"quantity\":5}", "\"quantity\":5},{\"productId\":999999,\"quantity\":1}");
        expectError(put("/sales-orders/" + order).contentType(MediaType.APPLICATION_JSON).content(update), 404, "/sales-orders/" + order);
        mvc.perform(get("/sales-orders/{id}", order)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("VENDA-001")).andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.products", hasSize(1))).andExpect(jsonPath("$.products[0].quantity").value(1))
                .andExpect(jsonPath("$.totalAmount").value(10.00));
        mvc.perform(put("/products/{id}", product).contentType(MediaType.APPLICATION_JSON)
                        .content(product("Produto", "10.00", supplier))).andExpect(status().isOk());
        expectError(post("/sales-orders").contentType(MediaType.APPLICATION_JSON)
                .content(order("VENDA-002", customer, product, 1)), 400, "/sales-orders");
        expectError(put("/sales-orders/" + order).contentType(MediaType.APPLICATION_JSON)
                .content(order("VENDA-ALTERADA", customer, product, 1)), 400, "/sales-orders/" + order);
        mvc.perform(get("/sales-orders/{id}", order)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("VENDA-001"));
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test
    void referencedResourcesReturnSanitizedConflictErrors() throws Exception {
        long supplier = createPartner("Fornecedor", "12345678901", "SUPPLIER");
        long customer = createPartner("Cliente", "12345678902", "CUSTOMER");
        long product = createProduct("Produto", "10.00", supplier);
        createOrder("VENDA-001", customer, product);
        expectError(delete("/business-partners/" + supplier), 409, "/business-partners/" + supplier);
        expectError(delete("/business-partners/" + customer), 409, "/business-partners/" + customer);
        expectError(delete("/products/" + product), 409, "/products/" + product);
        assertThat(partners.count()).isEqualTo(2);
        assertThat(products.count()).isEqualTo(1);
    }

    @Test
    void malformedRequestsAndUnknownRoutesUseConsistentErrors() throws Exception {
        expectError(post("/products").contentType(MediaType.APPLICATION_JSON).content("{"), 400, "/products");
        expectError(post("/business-partners").contentType(MediaType.APPLICATION_JSON)
                .content(partner("Cliente", "12345678901", "CUSTOMER").replace("INDIVIDUAL", "UNKNOWN")), 400, "/business-partners");
        expectError(get("/products/invalid-id"), 400, "/products/invalid-id");
        expectError(get("/business-partners/search"), 400, "/business-partners/search");
        expectError(get("/unknown-route"), 404, "/unknown-route");
        expectError(patch("/products/1"), 405, "/products/1");
        expectError(post("/products").contentType(MediaType.TEXT_PLAIN).content("text"), 415, "/products");
    }

    @Test
    void swaggerUiAndOpenApiDocumentAllResourcesAndDtos() throws Exception {
        JsonNode docs = json(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Hulysses One API")).andReturn());
        assertThat(docs.path("paths").size()).isEqualTo(14);
        assertThat(docs.path("paths").path("/business-partners/search").path("get").path("summary").asText()).contains("nome");
        assertThat(docs.path("components").path("schemas").has("ProductRequest")).isTrue();
        assertThat(docs.path("components").path("schemas").has("SalesOrderResponse")).isTrue();
        assertThat(docs.path("components").path("schemas").has("ApiError")).isTrue();
        assertThat(docs.path("paths").path("/products").path("post").path("responses").has("201")).isTrue();
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk()).andExpect(jsonPath("$.url").value("/v3/api-docs"));
    }

    private void expectError(MockHttpServletRequestBuilder request, int expectedStatus, String path) throws Exception {
        MvcResult result = mvc.perform(request).andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.status").value(expectedStatus)).andExpect(jsonPath("$.error").isNotEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty()).andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.timestamp").exists()).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("stackTrace", "SQLException", "Hibernate", "org.springframework", "br.com.");
    }

    private long createPartner(String name, String document, String role) throws Exception {
        return json(mvc.perform(post("/business-partners").contentType(MediaType.APPLICATION_JSON)
                .content(partner(name, document, role))).andExpect(status().isCreated()).andReturn()).path("id").asLong();
    }

    private long createProduct(String name, String price, long supplier) throws Exception {
        return json(mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON)
                .content(product(name, price, supplier))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true)).andReturn()).path("id").asLong();
    }

    private long createOrder(String number, long customer, long product) throws Exception {
        return json(mvc.perform(post("/sales-orders").contentType(MediaType.APPLICATION_JSON)
                .content(order(number, customer, product, 1))).andExpect(status().isCreated()).andReturn()).path("id").asLong();
    }

    private JsonNode json(MvcResult result) {
        return mapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String partner(String name, String document, String role) {
        return """
                {"name":"%s","document":"%s","email":"cliente@example.com","phone":"11999999999",
                 "type":"%s","roles":["%s"],"addresses":[%s],"active":false}
                """.formatted(name, document, document.length() == 14 ? "COMPANY" : "INDIVIDUAL", role, address());
    }

    private String address() {
        return """
                {"street":"Rua A","number":"10","neighborhood":"Centro","city":"Sao Paulo",
                 "state":"SP","country":"Brasil","postalCode":"01001000"}
                """.strip();
    }

    private String product(String name, String price, long supplier) {
        return """
                {"name":"%s","description":"Descricao","price":%s,"supplierId":%d,"active":false}
                """.formatted(name, price, supplier);
    }

    private String order(String number, long customer, long product, int quantity) {
        return """
                {"orderNumber":"%s","orderDate":"2026-10-03T10:00:00","status":"OPEN","customerId":%d,
                 "items":[{"productId":%d,"quantity":%d}]}
                """.formatted(number, customer, product, quantity);
    }
}
