package br.com.hulysses.business_partner_service.businesspartner.messaging;

import br.com.hulysses.business_partner_service.businesspartner.application.BusinessPartnerActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true", matchIfMissing = true)
public class BusinessPartnerActivityConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(BusinessPartnerActivityConsumer.class);
    private final BusinessPartnerActivityService activities;

    public BusinessPartnerActivityConsumer(BusinessPartnerActivityService activities) {
        this.activities = activities;
    }

    @RabbitListener(queues = "${messaging.business-partner.queue}",
            autoStartup = "${messaging.consumer.enabled:false}")
    public void consume(BusinessPartnerCreatedEvent event) {
        if (event.eventId() == null || event.eventId().length() != 36 || event.businessPartnerId() == null
                || event.businessPartnerId() <= 0 || event.occurredAt() == null
                || !"BUSINESS_PARTNER_CREATED".equals(event.eventType())) {
            throw new AmqpRejectAndDontRequeueException("Invalid business partner activity event");
        }
        // The service transaction commits before the listener returns and AMQP acknowledges.
        activities.record(event);
        LOG.info("Processed {} eventId={} businessPartnerId={}", event.eventType(), event.eventId(),
                event.businessPartnerId());
    }
}
