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
public class MessageBridgeFlow2 {

    @Inject
    @ConfigProperty(name = "mp.messaging.incoming.flow-2-to-1-input.enabled")
    String enabled;

    @Inject
    @ConfigProperty(name = "mp.messaging.outgoing.flow-2-to-1-output.destination")
    String flow2OutputQueueName;

    @Inject
    @Identifier("bridge-connection1-factory")
    ConnectionFactory jmsConnectionFactory1;

    JMSContext jmsContext;
    Queue FLOW_TWO_TO_ONE_OUTPUT;

    void logStartUp(@Observes final StartupEvent event) {
        if (enabled.equals("true")) {
            log.info("Starting up FLOW 2");
            jmsContext = jmsConnectionFactory1.createContext();
            FLOW_TWO_TO_ONE_OUTPUT = jmsContext.createQueue(flow2OutputQueueName);
        } else {
            log.info("Disabled FLOW 2");
        }
    }

    @Incoming("flow-2-to-1-input")
    @Outgoing("flow-2-to-1-ack")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    @Blocking
    public Message<Map<String, Object>> bridgeFromBroker1ToBroker2(final Message<Map<String, Object>> message) {
        log.info("Bridge from broker 2 to broker 1 for FLOW 1 PROCESSING MESSAGE");
        var result = MessageSender.sendJmsMapMessage(jmsContext, message, FLOW_TWO_TO_ONE_OUTPUT);
        if( result instanceof Failure<?> failure ) {
            log.error("Failed to send message to broker 1 -> error ", failure.value());
            var nack = message.nack(failure.error()); // Return the original message for retry or further processing
            return null;
        }
        log.info("Bridge from broker 2 to broker 1 for FLOW 2 PROCESSED MESSAGE");
        return message;
    }

    @Incoming("flow-2-to-1-ack")
    public CompletionStage<Void> ackBridgeFromBroker2ToBroker1(final Message<Map<String, Object>> message) {
        log.info("Acking the bridgeFromBroker2ToBroker1 message");
        return message.ack();
    }


}
