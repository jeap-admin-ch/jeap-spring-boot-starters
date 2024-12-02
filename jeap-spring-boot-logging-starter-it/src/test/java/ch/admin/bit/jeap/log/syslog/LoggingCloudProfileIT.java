package ch.admin.bit.jeap.log.syslog;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ActiveProfiles("cloud")
@Slf4j
@AutoConfigureObservability
public class LoggingCloudProfileIT extends SyslogIntegrationTestBase {

    @Test
    @StdIo
    void when_cloudProfileIsActive_then_shouldLogJsonToConsole(StdOut stdOut) {
        String logMessage = "Some message logged using cloud profile to console";
        log.info(logMessage);

        Map<String, String> msg = awaitAndAssertOneJsonLogEntryOnStdOutContaining(logMessage, stdOut);

        assertEquals(logMessage, msg.get("message"));
        assertEquals("test-app", msg.get("app"));
        assertNull(msg.get("logrelayHost"), "Context properties should not be logged");

        requestWithPrometheusRole()
                .get("/actuator/prometheus")
                .then().assertThat()
                .statusCode(200)
                // Makre sure there is no connection to the syslog server when using only the cloud profile
                .body(containsString("logging_distlog_connection_established_total 0.0"));
    }
}
