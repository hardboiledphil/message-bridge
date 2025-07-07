package com.acme;

import io.smallrye.reactive.messaging.jms.IncomingJmsMessageMetadata;
import io.smallrye.reactive.messaging.jms.JmsProperties;
import io.smallrye.reactive.messaging.jms.OutgoingJmsMessageMetadata;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.Map;

@Slf4j
public class MetadataMapper {

    public static OutgoingJmsMessageMetadata getOutgoingJmsMessageMetadata(Message<Map<String,Object>> jmsMessage) {
        IncomingJmsMessageMetadata incomingMetadata = jmsMessage.getMetadata(IncomingJmsMessageMetadata.class).orElseThrow();
        final var propNames = incomingMetadata.getPropertyNames();
        final var outgoingMetadataBuilder = OutgoingJmsMessageMetadata.builder();
        final var jmsPropertiesBuilder = JmsProperties.builder();
        while (propNames.hasMoreElements()) {
            String propName = (String) propNames.nextElement();
            String propValue = incomingMetadata.getStringProperty(propName);
            jmsPropertiesBuilder.with(propName, propValue);
        }
        outgoingMetadataBuilder.withProperties(jmsPropertiesBuilder.build());
        outgoingMetadataBuilder.withCorrelationId(incomingMetadata.getCorrelationId());
        log.info("type: {}", incomingMetadata.getType());
        return outgoingMetadataBuilder.build();
    }

    public static void addRequiredProperties(final MapMessage jmsMapMessage,
                                             final IncomingJmsMessageMetadata metadata) {
        metadata.getPropertyNames().asIterator().forEachRemaining(propName -> {
            String propValue = metadata.getStringProperty(propName);
            try {
                jmsMapMessage.setStringProperty(propName, propValue);
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
            log.info("Mapped property: {} with value: {}", propName, propValue);
        });
    }
}
