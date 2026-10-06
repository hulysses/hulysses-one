package br.com.hulysses.business_partner_service.businesspartner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "business_partner_activity")
public class BusinessPartnerActivity {
    @Id
    @Column(length = 36)
    private String eventId;
    @Column(nullable = false)
    private Long businessPartnerId;
    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false)
    private Instant occurredAt;
    @Column(nullable = false)
    private Instant processedAt;

    protected BusinessPartnerActivity() { }

    public BusinessPartnerActivity(String eventId, Long businessPartnerId, String eventType, Instant occurredAt) {
        this.eventId = eventId;
        this.businessPartnerId = businessPartnerId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.processedAt = Instant.now();
    }

    public String getEventId() { return eventId; }
    public Long getBusinessPartnerId() { return businessPartnerId; }
    public String getEventType() { return eventType; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getProcessedAt() { return processedAt; }
}
