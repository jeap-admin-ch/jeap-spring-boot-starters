package ch.admin.bit.jeap.log.syslog;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@AutoConfigureMetrics
class LoggingNoProfileIT extends LogIntegrationTestBase {

    @Test
    @StdIo
    void when_noSpecificProfileIsActive_then_shouldLogHumanReadableLinesToConsole(StdOut stdOut) {
        String logMessage = "Some message logged using cloud profile to console";
        log.info(logMessage);

        String msg = awaitAndAssertOneLogEntryOnStdOutContaining(logMessage, stdOut);
        assertTrue(msg.endsWith(logMessage), msg);

        requestWithPrometheusRole()
                .get("/actuator/prometheus")
                .then().assertThat()
                .statusCode(200)
                // Make sure there is no connection to the syslog server when no profile is active
                .body(containsString("logging_distlog_connection_established_total 0.0"));
    }
}
