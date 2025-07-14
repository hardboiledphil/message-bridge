package com.acme;

import io.smallrye.reactive.messaging.jms.IncomingJmsMessageMetadata;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Queue;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.Map;

@Slf4j
public class MessageSender {

    public static final String CSL_DATA = "cslData";

    public static Result<MapMessage> sendJmsMapMessage(final JMSContext jmsContext,
                                                       final Message<Map<String, Object>> incomingMessage,
                                                       final Queue destination){
        MapMessage outgoingMessage = null;
        String queueName = "";
        try {
            queueName = destination.getQueueName();
            IncomingJmsMessageMetadata incomingMetadata = incomingMessage.getMetadata(IncomingJmsMessageMetadata.class)
                    .orElseThrow(() -> new JMSException("IncomingJmsMessageMetadata not found"));
            outgoingMessage = jmsContext.createMapMessage();
            outgoingMessage.setString("cslData", (String) incomingMessage.getPayload().get(CSL_DATA));
            MetadataMapper.addRequiredProperties(outgoingMessage, incomingMetadata);
            jmsContext.createProducer().send(destination, outgoingMessage);
        } catch (JMSException e) {
            log.error("Error sending JMS MapMessage to destination: {}", queueName, e);
            return new Failure<>(e);
        }
        log.info("Message sent to destination: {}", queueName);
        return new Success<>(outgoingMessage);
    }
}
