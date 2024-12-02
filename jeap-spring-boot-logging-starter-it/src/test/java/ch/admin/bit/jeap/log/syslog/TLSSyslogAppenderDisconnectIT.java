package ch.admin.bit.jeap.log.syslog;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.backoff.ExponentialBackOff;

import static org.hamcrest.Matchers.containsString;

@ActiveProfiles("logrelay")
@Slf4j
@AutoConfigureObservability
public class TLSSyslogAppenderDisconnectIT extends SyslogIntegrationTestBase {

    @Test
    void when_messageIsLoggedAndSyslogNotAvailable_then_shouldIncreaseErrorMetrics() throws Exception {
        String loggedMessageBeforeShutdown = "Some message before server shutdown";
        log.info(loggedMessageBeforeShutdown);
        awaitAndAssertOneJsonLogEntryOnSyslogContaining(loggedMessageBeforeShutdown);

        mockSslServer.stop();

        String prefix = "Overflow TCP write buffer";
        for (int i = 0; i < 5000; i++) {
            log.info(prefix + " by stuffing lots of log messages into the stream and thus" +
                    "triggering a write error at some point " + i);
        }

        String loggedMessageAfterShutdown = "Some message logged after server shutdown";
        log.info(loggedMessageAfterShutdown);

        // Force a reconnect error by waiting for the reconnect interval to expire and then logging again
        Thread.sleep(ExponentialBackOff.DEFAULT_INITIAL_INTERVAL * 3);
        String messageToForceReconnect = "Message to force reconnect error";
        log.info(messageToForceReconnect);

        requestWithPrometheusRole()
                .get("/actuator/prometheus")
                .then().assertThat()
                .statusCode(200)
                .body(
                        // Expect a single successful initial connection attempt
                        containsString("logging_distlog_connection_established_total 1.0"),
                        // Expect the syslog logger to attempt to reconnect > 0 times
                        containsCounterBiggerThanZero("logging_distlog_connection_error_total"),
                        // Expect the syslog logger to attempt to reconnect > 0 times
                        containsCounterBiggerThanZero("logging_distlog_transmit_error_total"));
    }
}
