package br.com.hulysses.business_partner_service.businesspartner.messaging;

import java.time.Instant;
import java.util.UUID;

public record BusinessPartnerCreatedEvent(String eventId, String eventType, Long businessPartnerId,
                                         Instant occurredAt) {
    public static BusinessPartnerCreatedEvent forPartner(Long id) {
        return new BusinessPartnerCreatedEvent(UUID.randomUUID().toString(), "BUSINESS_PARTNER_CREATED", id,
                Instant.now());
    }
}
