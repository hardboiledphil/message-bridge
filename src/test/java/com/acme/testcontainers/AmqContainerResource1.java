package com.acme.testcontainers;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ApplicationScoped
@Slf4j
public class AmqContainerResource1 implements QuarkusTestResourceLifecycleManager {

    private static AmqContainer amqContainer1;
    public static final String ARTEMIS_CLIENT_TRUSTSTORE_JKS = "artemis/client-truststore.jks";
    public static final String CLIENT_TRUSTSTORE_PASSWORD = "middleware";
    public static final String NETWORK_HOST = "localhost";
    public static final String AMQ_LOGIN_CRED_1 = "artemis1";
    public static final String AMQ_LOGIN_PASSWORD_1 = "artemis1";

    public AmqContainerResource1() {
        log.info("default constructor called for AmqContainerResource1");
    }

    @Override
    public Map<String, String> start() {
        if (amqContainer1 == null) {
            amqContainer1 = new AmqContainer(AMQ_LOGIN_CRED_1, AMQ_LOGIN_PASSWORD_1)
                    .withNetwork(TestNetworkResource.getNetwork())
                    .withNetworkAliases(AmqContainer.NETWORK_NAME);
        }
        amqContainer1.start();

        await().pollDelay(1000, TimeUnit.MILLISECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .timeout(5000, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertTrue(amqContainer1.isRunning()));

        log.info("AmqContainer1.getHost() => {}:{}", amqContainer1.getHost(), getNetworkPort());
        log.info("AmqContainer1 artemis1.username =>  {}", AMQ_LOGIN_CRED_1);
        log.info("AmqContainer1 artemis1.password =>  {}", AMQ_LOGIN_PASSWORD_1);
        return Map.of(
                "jms1.host", getNetworkHost(),
                "jms1.port", getNetworkPort(),
                "jms.truststore.path", ARTEMIS_CLIENT_TRUSTSTORE_JKS,
                "jms.truststore.password", CLIENT_TRUSTSTORE_PASSWORD,
                "artemis1.username", AMQ_LOGIN_CRED_1,
                "artemis1.password", AMQ_LOGIN_PASSWORD_1
        );
    }

    @Override
    public void stop() {
        log.info("CONTAINER -> Stopping AMQ Container 1");
        if (amqContainer1 != null) {
            amqContainer1.stop();
        }
    }

    public void close() {
        amqContainer1.close();
    }

    public String getHost() {
        return amqContainer1.getHost();
    }

    public String getCorePort() {
        return Integer.toString(amqContainer1.getMappedPort(AmqContainer.AMQ_TCP_PORT));
    }

    public String getNetworkHost() {
        return NETWORK_HOST;
    }

    public String getNetworkPort() {
        return Integer.toString(amqContainer1.getMappedPort(AmqContainer.AMQ_TCP_PORT));
    }


}
