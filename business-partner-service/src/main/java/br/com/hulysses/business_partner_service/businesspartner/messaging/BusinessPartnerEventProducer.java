package br.com.hulysses.business_partner_service.businesspartner.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true", matchIfMissing = true)
public class BusinessPartnerEventProducer {
    private static final Logger LOG = LoggerFactory.getLogger(BusinessPartnerEventProducer.class);
    private final RabbitTemplate rabbit;
    private final String exchange;
    private final String routingKey;

    public BusinessPartnerEventProducer(RabbitTemplate rabbit,
            @Value("${messaging.business-partner.exchange}") String exchange,
            @Value("${messaging.business-partner.routing-key}") String routingKey) {
        this.rabbit = rabbit;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    // Publish only after the partner (or the Batch chunk) has actually committed.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(BusinessPartnerCreatedEvent event) {
        try {
            rabbit.convertAndSend(exchange, routingKey, event, message -> {
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                message.getMessageProperties().setMessageId(event.eventId());
                return message;
            });
            LOG.info("Published {} eventId={} businessPartnerId={}", event.eventType(), event.eventId(),
                    event.businessPartnerId());
        } catch (AmqpException exception) {
            // Academic best-effort delivery: the committed registration must still succeed.
            LOG.error("Activity publication failed; partner remains saved. eventId={} businessPartnerId={}",
                    event.eventId(), event.businessPartnerId(), exception);
        }
    }
}
