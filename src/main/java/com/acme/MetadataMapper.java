package com.acme;

import io.smallrye.reactive.messaging.jms.IncomingJmsMessageMetadata;
import io.smallrye.reactive.messaging.jms.JmsProperties;
import io.smallrye.reactive.messaging.jms.OutgoingJmsMessageMetadata;
import org.eclipse.microprofile.reactive.messaging.Message;

public class MetadataMapper {

    public static OutgoingJmsMessageMetadata getOutgoingJmsMessageMetadata(Message<String> jmsMessage) {
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
        return outgoingMetadataBuilder.build();
    }
}
