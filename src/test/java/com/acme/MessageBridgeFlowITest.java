package com.acme;

import com.acme.testcontainers.AmqContainerResource1;
import com.acme.testcontainers.AmqContainerResource2;
import com.acme.testcontainers.utils.ResourceReader;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.common.annotation.Identifier;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQDestination;
import org.apache.activemq.artemis.jms.client.ActiveMQMapMessage;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@QuarkusTest
@QuarkusTestResource(value = AmqContainerResource1.class, parallel = true)
@QuarkusTestResource(value = AmqContainerResource2.class, parallel = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class MessageBridgeFlowITest implements QuarkusTestAwaitility {

    @Inject
    @Identifier("bridge-connection1-factory")
    ConnectionFactory jmsConnectionFactory1;

    @Inject
    @Identifier("bridge-connection2-factory")
    ConnectionFactory jmsConnectionFactory2;

    @Inject
    @ConfigProperty(name = "mp.messaging.incoming.flow-1-to-2-input.destination")
    String flow1InputQueueName;

    @Inject
    @ConfigProperty(name = "mp.messaging.outgoing.flow-1-to-2-output.destination")
    String flow1OutputQueueName;

    @Inject
    @ConfigProperty(name = "mp.messaging.incoming.flow-2-to-1-input.destination")
    String flow2InputQueueName;

    @Inject
    @ConfigProperty(name = "mp.messaging.outgoing.flow-2-to-1-output.destination")
    String flow2OutputQueueName;

    @Test
    void testFlow1To2() throws JMSException {

        var testControlMessage1 = ResourceReader.readResourceToString("message.xml");
        assertNotNull(testControlMessage1, "testControlMessage is null");
        assertNotEquals("", testControlMessage1, "testControlMessage is empty");
        var testControlMessageMetadata = ResourceReader.readResourceToString("message.metadata");
        assertNotNull(testControlMessageMetadata, "testControlMessageMetadata is null");
        assertNotEquals("", testControlMessageMetadata, "testControlMessageMetadata is empty");
        var headerProperties = ResourceReader.getHeaderProperties(testControlMessageMetadata);
        var customProperties = ResourceReader.getCustomProperties(testControlMessageMetadata);
        var jmsxProperties = ResourceReader.getJmsExtensionsProperties(testControlMessageMetadata);

        log.info("jmsxProperties -> {}", jmsxProperties);

        log.info("Creating sender for FLOW1 on queue: {}", flow1InputQueueName);
        try (JMSContext context = jmsConnectionFactory1.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
            MapMessage message = context.createMapMessage();
            message.setString("cslData", testControlMessage1);
            addMetadataToTestMessage(headerProperties, customProperties, message);
            context.createProducer().send(context.createQueue(flow1InputQueueName), message);
        }

        final List<Message> messages = new ArrayList<>();
        log.info("Creating receiver for FLOW1 on queue: {}", flow1OutputQueueName);
        try (JMSContext context = jmsConnectionFactory2.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
            JMSConsumer consumer = context.createConsumer(context.createQueue(flow1OutputQueueName));
            while (true) {
                Message message = consumer.receive();
                if (message == null) continue;
                log.info("message received on FLOW1");
                messages.add(message);
                break;
            }
        }
        await("assert FLOW1 message").untilAsserted(() -> {
            assertEquals(1, messages.size());
            ActiveMQMapMessage message = (ActiveMQMapMessage) messages.getFirst();
            assertEquals(testControlMessage1, message.getString("cslData"));
            assertMessageMetadata(headerProperties, customProperties, jmsxProperties, message);
            log.info("assert flow1 message - PASSED");
        });
    }

    @Test
    void testFlow2To1() throws JMSException {

        var testControlMessage2 = ResourceReader.readResourceToString("message.xml");
        assertNotNull(testControlMessage2, "testControlMessage is null");
        assertNotEquals("", testControlMessage2, "testControlMessage is empty");
        var testControlMessageMetadata = ResourceReader.readResourceToString("message.metadata");
        assertNotNull(testControlMessageMetadata, "testControlMessageMetadata is null");
        assertNotEquals("", testControlMessageMetadata, "testControlMessageMetadata is empty");
        var headerProperties = ResourceReader.getHeaderProperties(testControlMessageMetadata);
        var customProperties = ResourceReader.getCustomProperties(testControlMessageMetadata);
        var jmsxProperties = ResourceReader.getJmsExtensionsProperties(testControlMessageMetadata);

        log.info("jmsxProperties -> {}", jmsxProperties);

        log.info("Creating sender for FLOW2 on queue: {}", flow2InputQueueName);
        try (JMSContext context = jmsConnectionFactory2.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
            MapMessage message = context.createMapMessage();
            message.setString("cslData", testControlMessage2);
            addMetadataToTestMessage(headerProperties, customProperties, message);
            context.createProducer().send(context.createQueue(flow2InputQueueName), message);
        }

        final List<Message> messages = new ArrayList<>();
        log.info("Creating receiver for FLOW2 on queue: {}", flow2OutputQueueName);
        try (JMSContext context = jmsConnectionFactory1.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
            JMSConsumer consumer = context.createConsumer(context.createQueue(flow2OutputQueueName));
            while (true) {
                Message message = consumer.receive();
                if (message == null) continue;
                log.info("message received on FLOW2");
                messages.add(message);
                break;
            }
        }
        await("assert FLOW2 message").untilAsserted(() -> {
            assertEquals(1, messages.size());
            ActiveMQMapMessage message = (ActiveMQMapMessage) messages.getFirst();
            assertEquals(testControlMessage2, message.getString("cslData"));
            assertMessageMetadata(headerProperties, customProperties, jmsxProperties, message);
            log.info("assert flow2 message - PASSED");
        });
    }

    private static void assertMessageMetadata(final Map<String, String> headerProperties,
                                              final Map<String, String> customProperties,
                                              final Map<String, String> jmsxProperties,
                                              final Message message) throws JMSException {
        log.info("=== Message metadata validation ===");

        for (Map.Entry<String, String> prop : headerProperties.entrySet()) {
            if ("null".equalsIgnoreCase(prop.getValue())) {
                continue;
            }

            switch (prop.getKey()) {
                case "JMSCorrelationID" -> assertEquals(prop.getValue(), message.getJMSCorrelationID());
                case "JMSDeliveryMode" -> assertEquals(Integer.parseInt(prop.getValue()), message.getJMSDeliveryMode());
                case "JMSDeliveryTime" -> assertEquals(Long.parseLong(prop.getValue()), message.getJMSDeliveryTime());
                case "JMSExpiration" -> assertEquals(Long.parseLong(prop.getValue()), message.getJMSExpiration());
                case "JMSPriority" -> assertEquals(Integer.parseInt(prop.getValue()), message.getJMSPriority());
                case "JMSRedelivered" -> assertEquals(Boolean.parseBoolean(prop.getValue()), message.getJMSRedelivered());
//                case "JMSReplyTo" -> {
//                    var destination = (ActiveMQDestination) message.getJMSReplyTo();
//                    assertEquals(prop.getValue(), destination.getAddress());
//                }
                case "JMSDestination" -> {
                    var destination = (ActiveMQDestination) message.getJMSDestination();
                    assertEquals(prop.getValue(), destination.getAddress());
                }
                case "JMSTimestamp", "JMSType", "JMSMessageID" -> {
                    // Do nothing - Created via producer or overridden by broker
                }
            }
        }
        log.info("jms header properties validation - PASSED");

        var props = customProperties.entrySet();

        log.info("customProps -> {}", props);
        props.stream()
                .filter(entry -> !entry.getKey().startsWith("_"))
                .forEach(entry -> {
                    var key = entry.getKey();
                    var value = entry.getValue();
                    Object destValue;
                    try {
                        destValue = message.getObjectProperty(key);
                        log.info("checking customProperty {} old -> {} new -> {}", key, value, destValue);
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }
                    assertEquals(value, destValue, "Custom property not found in output message");
                });

        log.info("custom properties validation - PASSED");

        for (Map.Entry<String, String> prop : jmsxProperties.entrySet()) {
            var trimmedIfNotNull = prop.getKey() != null ? prop.getKey().trim() : null;
            log.debug("asserting jmsxProperty {}=({},{})", trimmedIfNotNull, prop.getValue(), message.getObjectProperty(trimmedIfNotNull));
            switch (Objects.requireNonNull(trimmedIfNotNull)) {
                case "JMSXDeliveryCount" ->
                        assertEquals(Integer.parseInt(prop.getValue().trim()), message.getObjectProperty(trimmedIfNotNull));
                case "JMSXGroupSeq" ->
                        assertEquals(Integer.parseInt(prop.getValue().trim()), message.getObjectProperty(trimmedIfNotNull));
                default -> throw new RuntimeException("not expecting jms extension called " + prop.getKey());
            }
        }
        log.info("jms extension properties validation - PASSED");
    }

    private static void addMetadataToTestMessage(final Map<String, String> headerProperties,
                                                 final Map<String, String> customProperties,
                                                 final Message message) throws JMSException {
        for (Map.Entry<String, String> prop : headerProperties.entrySet()) {
            if ("null".equalsIgnoreCase(prop.getValue())) {
                continue;
            }

            log.trace("Adding headerProperty -> {}={}", prop.getKey(), prop.getValue());

            switch (prop.getKey()) {
                case "JMSCorrelationID" -> message.setJMSCorrelationID(prop.getValue());
                case "JMSDeliveryMode" -> message.setJMSDeliveryMode(Integer.parseInt(prop.getValue()));
                case "JMSDeliveryTime" -> message.setJMSDeliveryTime(Long.parseLong(prop.getValue()));
                case "JMSExpiration" -> message.setJMSExpiration(Long.parseLong(prop.getValue()));
                case "JMSPriority" -> message.setJMSPriority(Integer.parseInt(prop.getValue()));
                case "JMSRedelivered" -> message.setJMSRedelivered(Boolean.parseBoolean(prop.getValue()));
                case "JMSReplyTo" -> message.setJMSReplyTo(new ActiveMQQueue(prop.getValue()));
                case "JMSTimestamp" -> message.setJMSTimestamp(Long.parseLong(prop.getValue()));
                case "JMSDestination", "JMSType", "JMSMessageID" -> { } // Do nothing - Created via producer or
                // will be overridden by broker
            }
        }

        for (Map.Entry<String, String> prop : customProperties.entrySet()) {
            log.trace("Adding customProperty -> {}={}", prop.getKey(), prop.getValue());
            message.setObjectProperty(prop.getKey(), prop.getValue());
        }
    }

}
