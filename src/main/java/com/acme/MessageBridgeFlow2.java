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
public class MessageBridgeFlow2 {

    @Inject
    @ConfigProperty(name = "mp.messaging.incoming.flow-2-to-1-input.enabled")
    String enabled;

    @Inject
    @Identifier("bridge-connection1-factory")
    ConnectionFactory jmsConnectionFactory1;

    JMSContext jmsContext;
    Queue FLOW_TWO_TO_ONE_OUTPUT;

    void logStartUp(@Observes final StartupEvent event) {
        if (enabled.equals("true")) {
            log.info("Starting up FLOW 2");
            jmsContext = jmsConnectionFactory1.createContext();
            FLOW_TWO_TO_ONE_OUTPUT = jmsContext.createQueue(Queues.FLOW_TWO_TO_ONE_OUTPUT);
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
        var result = sendJmsMapMessage(message, FLOW_TWO_TO_ONE_OUTPUT);
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
