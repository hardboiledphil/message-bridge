package com.acme;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;

import java.time.Duration;

public interface QuarkusTestAwaitility {
    Duration POLL_DELAY = Duration.ofMillis(250);
    Duration POLL_INTERVAL = Duration.ofMillis(250);
    Duration TIMEOUT = Duration.ofMillis(5000);

    default ConditionFactory await(String alias) {
        return Awaitility.await(alias).pollDelay(POLL_DELAY).pollInterval(POLL_INTERVAL).timeout(TIMEOUT);
    }
}
