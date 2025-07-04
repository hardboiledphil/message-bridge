package com.acme.testcontainers;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class AmqContainer extends GenericContainer<AmqContainer> {

    public static final DockerImageName DEFAULT_IMAGE_NAME =
            DockerImageName.parse("apache/activemq-artemis:2.41.0");

    public static final String NETWORK_NAME = "AMQ";

    public static final int AMQ_TCP_PORT = 61616;
    public static final int AMQ_SSL_PORT = 61617;
    public static final int AMQ_ADMIN_PORT = 8161;

    public AmqContainer(final String login_cred, final String login_password) {
        super(DEFAULT_IMAGE_NAME);
        this.waitStrategy = new LogMessageWaitStrategy()
                .withRegEx(".*Server is now active.*\\s")
                .withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS));
        this.withClasspathResourceMapping("artemis/broker.xml",
                        "/opt/amq/conf/broker.xml", BindMode.READ_ONLY);
        this.withEnv("ARTEMIS_USER", login_cred);
        this.withEnv("ARTEMIS_PASSWORD", login_password);
        this.addExposedPorts(AMQ_TCP_PORT, AMQ_SSL_PORT, AMQ_ADMIN_PORT);
    }

}
