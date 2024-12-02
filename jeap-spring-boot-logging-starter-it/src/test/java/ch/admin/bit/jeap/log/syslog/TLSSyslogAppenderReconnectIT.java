package ch.admin.bit.jeap.log.syslog;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("logrelay")
@Slf4j
@AutoConfigureObservability
public class TLSSyslogAppenderReconnectIT extends SyslogIntegrationTestBase {

    @Test
    void when_messageIsLoggedAndSyslogNotAvailable_then_shouldReconnectWhenAvailableAgain() throws InterruptedException {
        String loggedMessageBeforeShutdown = "Some message before server shutdown";
        log.info(loggedMessageBeforeShutdown);
        awaitAndAssertOneJsonLogEntryOnSyslogContaining(loggedMessageBeforeShutdown);

        mockSslServer.stop();

        String prefix = "Overflow TCP write buffer";
        for (int i = 0; i < 1000; i++) {
            log.info(prefix + " by stuffing lots of log messages into the stream and thus" +
                    "triggering a write error at some point " + i);
        }
        String loggedMessageAfterShutdown = "Some message logged after server shutdown";
        log.info(loggedMessageAfterShutdown);

        mockSslServer.start(PORT);
        // Make sure reconnect interval is reached
        Thread.sleep(ExponentialBackOff.DEFAULT_INITIAL_INTERVAL * 3);

        for (int i = 0; i < 1000; i++) {
            log.info("Log some messages after server restart to trigger a reconnect " + i);
        }
        String loggedMessageAfterRestart = "Some message logged after server restart";
        log.info(loggedMessageAfterRestart);

        Map<String, String> msg = getMsg(awaitAndAssertOneJsonLogEntryOnSyslogContaining(loggedMessageAfterRestart));
        assertTrue(msg.get("logger").contains(getClass().getSimpleName()));
        assertEquals(Thread.currentThread().getName(), msg.get("thread_name"));

        requestWithPrometheusRole()
                .get("/actuator/prometheus")
                .then().assertThat()
                .statusCode(200)
                .body(
                        // Expect a second successful connection attempt after restarting the server
                        containsString("logging_distlog_connection_established_total 2.0"),
                        // Expect the syslog logger to attempt to reconnect > 0 times
                        containsCounterBiggerThanZero("logging_distlog_transmit_error_total"));
    }
}
