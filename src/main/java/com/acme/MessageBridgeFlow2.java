package com.acme;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import java.util.Map;

@Slf4j
@ApplicationScoped
public class MessageBridgeFlow2 {

    @Inject
    @ConfigProperty(name = "mp.messaging.incoming.flow-2-to-1-input.enabled")
    String enabled;

    void logStartUp(@Observes final StartupEvent event) {
        if (enabled.equals("true")) {
            log.info("Starting up FLOW 2");
        } else {
            log.info("Disabled FLOW 2");
        }
    }

    @Incoming("flow-2-to-1-input")
    @Outgoing("flow-2-to-1-output")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    public Message<Map<String,Object>> bridgeFromBroker2ToBroker1(final Message<Map<String,Object>> jmsMessage) {
        log.info("Bridge from broker 2 to broker 1 for FLOW 2");
        return Message.of(jmsMessage.getPayload(), () -> {
            log.info("Returning ack for broker 2 to broker 1 for FLOW 2");
            return jmsMessage.ack();
        }).addMetadata(MetadataMapper.getOutgoingJmsMessageMetadata(jmsMessage));
    }
}
