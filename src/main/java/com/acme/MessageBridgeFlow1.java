package com.acme;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.Map;
import java.util.concurrent.CompletionStage;

@Slf4j
@ApplicationScoped
public class MessageBridgeFlow1 {

    @Inject
    @ConfigProperty(name = "mp.messaging.incoming.flow-1-to-2-input.enabled")
    String enabled;

    @Inject
    @ConfigProperty(name = "mp.messaging.outgoing.flow-1-to-2-output.destination")
    String flow1OutputQueueName;

    @Inject
    @Identifier("bridge-connection2-factory")
    ConnectionFactory jmsConnectionFactory2;

    JMSContext jmsContext;
    Queue FLOW_ONE_TO_TWO_OUTPUT;

    void logStartUp(@Observes final StartupEvent event) {
        if (enabled.equals("true")) {
            log.info("Starting up FLOW 1");
            jmsContext = jmsConnectionFactory2.createContext();
            FLOW_ONE_TO_TWO_OUTPUT = jmsContext.createQueue(flow1OutputQueueName);
        } else {
            log.info("Disabled FLOW 1");
        }
    }

    @Incoming("flow-1-to-2-input")
    @Outgoing("flow-1-to-2-ack")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    @Blocking
    public Message<Map<String, Object>> bridgeFromBroker1ToBroker2(final Message<Map<String, Object>> message) {
        log.info("Bridge from broker 1 to broker 2 for FLOW 1 PROCESSING MESSAGE");
        var result = MessageSender.sendJmsMapMessage(jmsContext, message, FLOW_ONE_TO_TWO_OUTPUT);
        if( result instanceof Failure<?> failure ) {
            log.error("Failed to send message to broker 2 -> error ", failure.value());
            var nack = message.nack(failure.error()); // Return the original message for retry or further processing
            return null;
        }
        log.info("Bridge from broker 1 to broker 2 for FLOW 1 PROCESSED MESSAGE");
        return message;
    }

    @Incoming("flow-1-to-2-ack")
    public CompletionStage<Void> ackBridgeFromBroker1ToBroker2(final Message<Map<String, Object>> message) {
        log.info("Acking the bridgeFromBroker1ToBroker2 message");
        return message.ack();
    }

}
