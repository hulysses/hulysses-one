package br.com.hulysses.business_partner_service;

import br.com.hulysses.business_partner_service.businesspartner.application.BusinessPartnerService;
import br.com.hulysses.business_partner_service.businesspartner.messaging.BusinessPartnerActivityConsumer;
import br.com.hulysses.business_partner_service.businesspartner.messaging.BusinessPartnerCreatedEvent;
import br.com.hulysses.business_partner_service.businesspartner.persistence.BusinessPartnerActivityRepository;
import br.com.hulysses.business_partner_service.businesspartner.persistence.BusinessPartnerRepository;
import br.com.hulysses.business_partner_service.businesspartner.presentation.dto.BusinessPartnerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import static br.com.hulysses.business_partner_service.businesspartner.domain.BusinessPartnerRole.CUSTOMER;
import static br.com.hulysses.business_partner_service.businesspartner.domain.BusinessPartnerType.INDIVIDUAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"messaging.enabled=true", "spring.rabbitmq.dynamic=false",
        "messaging.consumer.enabled=false", "messaging.business-partner.exchange=business-partner.events",
        "messaging.business-partner.queue=business-partner.activities",
        "messaging.business-partner.routing-key=business-partner.created"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Stage4Tests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired BusinessPartnerService service;
    @Autowired BusinessPartnerRepository partners;
    @Autowired BusinessPartnerActivityRepository activities;
    @Autowired BusinessPartnerActivityConsumer consumer;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean RabbitTemplate rabbit;

    @BeforeEach
    void clean() {
        activities.deleteAll();
        partners.deleteAll();
        reset(rabbit);
    }

    @Test
    void registrationPublishesAfterCommitAndConsumerPersistsHistoryOnce() throws Exception {
        var partner = service.create(request("22000000001"));
        var event = ArgumentCaptor.forClass(BusinessPartnerCreatedEvent.class);
        verify(rabbit).convertAndSend(eq("business-partner.events"), eq("business-partner.created"),
                event.capture(), any(MessagePostProcessor.class));
        assertThat(partners.existsById(partner.id())).isTrue();
        assertThat(activities.count()).isZero();
        assertThat(event.getValue().businessPartnerId()).isEqualTo(partner.id());

        var converter = new JacksonJsonMessageConverter(
                "br.com.hulysses.business_partner_service.businesspartner.messaging");
        var message = converter.toMessage(event.getValue(), new org.springframework.amqp.core.MessageProperties());
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(new String(message.getBody(), StandardCharsets.UTF_8)).contains("BUSINESS_PARTNER_CREATED");
        var decoded = (BusinessPartnerCreatedEvent) converter.fromMessage(message);
        consumer.consume(decoded);
        consumer.consume(decoded);
        assertThat(activities.count()).isEqualTo(1);
        mvc.perform(get("/business-partners/{id}/activities", partner.id()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].eventType").value("BUSINESS_PARTNER_CREATED"));
    }

    @Test
    void rolledBackRegistrationDoesNotPublish() {
        new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
            service.create(request("22000000002"));
            transaction.setRollbackOnly();
        });
        assertThat(partners.existsByDocument("22000000002")).isFalse();
        verifyNoInteractions(rabbit);
    }

    @Test
    void brokerFailureDoesNotTurnCommittedRegistrationIntoHttpFailure() throws Exception {
        doThrow(new AmqpConnectException(new java.net.ConnectException("Broker unavailable")))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));
        mvc.perform(post("/business-partners").contentType("application/json")
                .content(mapper.writeValueAsString(request("22000000003")))).andExpect(status().isCreated());
        assertThat(partners.existsByDocument("22000000003")).isTrue();
    }

    @Test
    void sampleRunsInChunksWithJdbcMetadataAndReimportFiltersDuplicates() throws Exception {
        String result = mvc.perform(post("/batch/business-partners/import"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.readCount").value(13)).andExpect(jsonPath("$.writeCount").value(12))
                .andExpect(jsonPath("$.filterCount").value(1)).andReturn().getResponse().getContentAsString();
        assertThat(partners.count()).isEqualTo(12);
        assertThat(mapper.readTree(result).path("commitCount").asLong()).isGreaterThanOrEqualTo(2);
        long executionId = mapper.readTree(result).path("jobExecutionId").asLong();
        assertThat(jdbc.queryForObject("select STATUS from BATCH_JOB_EXECUTION where JOB_EXECUTION_ID = ?",
                String.class, executionId)).isEqualTo("COMPLETED");
        verify(rabbit, times(12)).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));
        mvc.perform(post("/batch/business-partners/import"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.writeCount").value(0))
                .andExpect(jsonPath("$.filterCount").value(13));
        assertThat(partners.count()).isEqualTo(12);
    }

    @Test
    void uploadedCsvNormalizesFieldsAndFiltersInvalidEnumsAndSameChunkDuplicates() throws Exception {
        var file = csv("""
                name,document,email,phone,type,roles
                 Normalized Name ,220.000.000-04,UPPER@EXAMPLE.COM, 11999990000 , individual , customer;supplier
                Duplicate,22000000004,other@example.com,11999990000,INDIVIDUAL,CUSTOMER
                Invalid,22000000005,other@example.com,11999990000,UNKNOWN,CUSTOMER
                """);
        mvc.perform(multipart("/batch/business-partners/import").file(file))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.writeCount").value(1)).andExpect(jsonPath("$.filterCount").value(2));
        var partner = partners.findAll().getFirst();
        assertThat(partner.getName()).isEqualTo("Normalized Name");
        assertThat(partner.getEmail()).isEqualTo("upper@example.com");
        assertThat(partner.getDocument()).isEqualTo("22000000004");
    }

    @Test
    void malformedCsvReportsFailureWithoutPersistingItsChunkOrPublishingEvents() throws Exception {
        mvc.perform(multipart("/batch/business-partners/import").file(csv("""
                name,document,email,phone,type,roles
                Valid,22000000006,test@example.com,11999990000,INDIVIDUAL,CUSTOMER
                broken,row
                """))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FAILED"));
        assertThat(partners.count()).isZero();
        verifyNoInteractions(rabbit);
        mvc.perform(multipart("/batch/business-partners/import").file(csv("name,document\nWrong,123\n")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FAILED"));
    }

    private BusinessPartnerRequest request(String document) {
        return new BusinessPartnerRequest("Stage 4 Partner", document, "stage4@example.com", "11999990000",
                INDIVIDUAL, Set.of(CUSTOMER), null, null);
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "partners.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
