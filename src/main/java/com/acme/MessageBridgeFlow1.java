package com.acme;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.jms.IncomingJmsMessageMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
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
    @Identifier("bridge-connection2-factory")
    ConnectionFactory jmsConnectionFactory2;

    JMSContext jmsContext;
    Queue FLOW_ONE_TO_TWO_OUTPUT;

    void logStartUp(@Observes final StartupEvent event) {
        if (enabled.equals("true")) {
            log.info("Starting up FLOW 1");
            jmsContext = jmsConnectionFactory2.createContext();
            FLOW_ONE_TO_TWO_OUTPUT = jmsContext.createQueue(Queues.FLOW_ONE_TO_TWO_OUTPUT);
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
        var result = sendJmsMapMessage(message, FLOW_ONE_TO_TWO_OUTPUT);
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

    private Result<MapMessage> sendJmsMapMessage(final Message<Map<String, Object>> jmsMessage, final Queue destination){
        MapMessage newMapMessage = null;
        String queueName = "";
        try {
            queueName = destination.getQueueName();
            IncomingJmsMessageMetadata incomingMetadata = jmsMessage.getMetadata(IncomingJmsMessageMetadata.class)
                    .orElseThrow(() -> new JMSException("IncomingJmsMessageMetadata not found"));
            newMapMessage = jmsContext.createMapMessage();
            newMapMessage.setString("cslData", (String) jmsMessage.getPayload().get("cslData"));
            MetadataMapper.mapRequiredProperties(newMapMessage, incomingMetadata);
            jmsContext.createProducer().send(destination, newMapMessage);
        } catch (JMSException e) {
            log.error("Error sending JMS MapMessage to destination: {}", queueName, e);
            return new Failure<>(e);
        }
        log.info("Message sent to destination: {}", queueName);
        return new Success<>(newMapMessage);

    }
}
