package com.acme;

import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.jms.ConnectionFactory;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
public class ConnectionFactoryBean {

    ConnectionFactory connectionFactory1;

    @ConfigProperty(name = "quarkus.artemis.server1.url")
    String amq1Url;
    @ConfigProperty(name = "quarkus.artemis.server1.username")
    String amq1User;
    @ConfigProperty(name = "quarkus.artemis.server1.password")
    String amq1Password;

    @Produces
    @Identifier("bridge-connection1-factory")
    ConnectionFactory connectionFactory1Factory() {
        if (connectionFactory1 == null) {
            log.info("ACTIVEMQ 1 CONNECTION CREATED => Amq1User:{} Amq1Url:{}", amq1User, amq1Url);
            return connectionFactory1 = new ActiveMQJMSConnectionFactory(amq1Url, amq1User, amq1Password);
        }
        log.debug("ACTIVEMQ CONNECTION 1 Supplied from cache");
        return connectionFactory1;
    }

    ConnectionFactory connectionFactory2;

    @ConfigProperty(name = "quarkus.artemis.server2.url")
    String amq2Url;
    @ConfigProperty(name = "quarkus.artemis.server2.username")
    String amq2User;
    @ConfigProperty(name = "quarkus.artemis.server2.password")
    String amq2Password;

    @Produces
    @Identifier("bridge-connection2-factory")
    ConnectionFactory connectionFactory2Factory() {
        if (connectionFactory2 == null) {
            log.info("ACTIVEMQ 2 CONNECTION CREATED => Amq1User:{} Amq1Url:{}", amq2User, amq2Url);
            return connectionFactory2 = new ActiveMQJMSConnectionFactory(amq2Url, amq2User, amq2Password);
        }
        log.debug("ACTIVEMQ CONNECTION 2 Supplied from cache");
        return connectionFactory2;
    }

}