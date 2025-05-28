package com.acme;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.jms.IncomingJmsMessageMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.messaging.Message;

@Slf4j
@ApplicationScoped
public class MessageBridgeFlow1 {

    @Inject
    @ConfigProperty(name = "mp.messaging.incoming.flow-1-to-2-input.enabled")
    String enabled;

    void logStartUp(@Observes final StartupEvent event) {
        if (enabled.equals("true")) {
            log.info("Starting up FLOW 1");
        } else {
            log.info("Disabled FLOW 1");
        }
    }

    @Incoming("flow-1-to-2-input")
    @Outgoing("flow-1-to-2-output")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    public Message<String> bridgeFromBroker1ToBroker2(final Message<String> jmsMessage) {
        log.info("Bridge from broker 1 to broker 2 for FLOW 1");
//        var metadata = jmsMessage.getMetadata(IncomingJmsMessageMetadata.class).orElseThrow();
        return Message.of(jmsMessage.getPayload(), () -> {
            log.info("Returning ack for broker 1 to broker 2 for FLOW 1");
            return jmsMessage.ack();
        });
    }

}
