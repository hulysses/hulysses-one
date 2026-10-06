package br.com.hulysses.business_partner_service.businesspartner.presentation.dto;

import br.com.hulysses.business_partner_service.businesspartner.domain.BusinessPartnerActivity;
import java.time.Instant;

public record BusinessPartnerActivityResponse(String eventId, Long businessPartnerId, String eventType,
                                             Instant occurredAt, Instant processedAt) {
    public static BusinessPartnerActivityResponse from(BusinessPartnerActivity activity) {
        return new BusinessPartnerActivityResponse(activity.getEventId(), activity.getBusinessPartnerId(),
                activity.getEventType(), activity.getOccurredAt(), activity.getProcessedAt());
    }
}
