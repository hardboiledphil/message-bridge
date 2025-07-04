package com.acme;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.Map;

@Slf4j
@ApplicationScoped
public class MessageBridgeFlow1 {

    @Inject
    @ConfigProperty(name = "mp.messaging.incoming.flow-1-to-2-input.enabled")
    String enabled;

    @Inject
    @Identifier("bridge-connection2-factory")
    ConnectionFactory jmsConnectionFactory2;

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
    public Message<Map<String,Object>> bridgeFromBroker1ToBroker2(final Message<Map<String,Object>> jmsMessage) {
        log.info("Bridge from broker 1 to broker 2 for FLOW 1");

        var incomingMetadata = jmsMessage.getMetadata();

        try (JMSContext context = jmsConnectionFactory2.createContext()) {
            MapMessage mapMessage = context.createMapMessage();
            mapMessage.setString("cslData", jmsMessage.getPayload().get("cslData"));

            // Wrap it in a SmallRye JMS Message so the extension does NOT convert it
//            return JmsMessage.of(mapMessage);
            return Message.of(mapMessage.getBody(Map<String, Object>.class), () -> {
                log.info("Returning ack for broker 1 to broker 2 for FLOW 1");
                return jmsMessage.ack();
            }).addMetadata(MetadataMapper.getOutgoingJmsMessageMetadata(jmsMessage));
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

    }

}
