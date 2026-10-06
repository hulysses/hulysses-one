package br.com.hulysses.business_partner_service.businesspartner.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMessagingConfig {
    @Bean
    Declarables businessPartnerTopology(@Value("${messaging.business-partner.exchange}") String exchange,
                                        @Value("${messaging.business-partner.queue}") String queue,
                                        @Value("${messaging.business-partner.routing-key}") String routingKey) {
        DirectExchange directExchange = new DirectExchange(exchange, true, false);
        Queue activityQueue = QueueBuilder.durable(queue).build();
        return new Declarables(directExchange, activityQueue,
                BindingBuilder.bind(activityQueue).to(directExchange).with(routingKey));
    }

    @Bean
    MessageConverter rabbitJsonMessageConverter() {
        return new JacksonJsonMessageConverter("br.com.hulysses.business_partner_service.businesspartner.messaging");
    }
}
