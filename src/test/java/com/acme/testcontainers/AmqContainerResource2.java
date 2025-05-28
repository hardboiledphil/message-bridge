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
public class AmqContainerResource2 implements QuarkusTestResourceLifecycleManager {

    private static AmqContainer amqContainer2;
    public static final String ARTEMIS_CLIENT_TRUSTSTORE_JKS = "artemis/client-truststore.jks";
    public static final String CLIENT_TRUSTSTORE_PASSWORD = "middleware";
    public static final String NETWORK_HOST = "localhost";
    public static final String AMQ_LOGIN_CRED_2 = "artemis2";
    public static final String AMQ_LOGIN_PASSWORD_2 = "artemis2";

    public AmqContainerResource2() {
        log.info("default constructor called for AmqContainerResource2");
    }

    @Override
    public Map<String, String> start() {
        if (amqContainer2 == null) {
            amqContainer2 = new AmqContainer(AMQ_LOGIN_CRED_2, AMQ_LOGIN_PASSWORD_2)
                    .withNetwork(TestNetworkResource.getNetwork())
                    .withNetworkAliases(AmqContainer.NETWORK_NAME);
        }
        amqContainer2.start();

        await().pollDelay(1000, TimeUnit.MILLISECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .timeout(5000, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertTrue(amqContainer2.isRunning()));

        log.info("AmqContainer2.getHost() => {}:{}", amqContainer2.getHost(), getNetworkPort());
        log.info("AmqContainer2 artemis2.username =>  {}", AMQ_LOGIN_CRED_2);
        log.info("AmqContainer2 artemis2.password =>  {}", AMQ_LOGIN_PASSWORD_2);
        return Map.of(
                "jms2.host", getNetworkHost(),
                "jms2.port", getNetworkPort(),
                "jms.truststore.path", ARTEMIS_CLIENT_TRUSTSTORE_JKS,
                "jms.truststore.password", CLIENT_TRUSTSTORE_PASSWORD,
                "artemis2.username", AMQ_LOGIN_CRED_2,
                "artemis2.password", AMQ_LOGIN_PASSWORD_2
        );
    }

    @Override
    public void stop() {
        log.info("CONTAINER -> Stopping AMQ Container 2");
        if (amqContainer2 != null) {
            amqContainer2.stop();
        }
    }

    public void close() {
        amqContainer2.close();
    }

    public String getHost() {
        return amqContainer2.getHost();
    }

    public String getCorePort() {
        return Integer.toString(amqContainer2.getMappedPort(AmqContainer.AMQ_TCP_PORT));
    }

    public String getNetworkHost() {
        return NETWORK_HOST;
    }

    public String getNetworkPort() {
        return Integer.toString(amqContainer2.getMappedPort(AmqContainer.AMQ_TCP_PORT));
    }


}
