package ch.admin.bit.jeap.log.syslog;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@TestPropertySource(properties = "jeap.logging.platform=cloudwatch")
public class LoggingCloudwatchIT extends LogIntegrationTestBase {

    @Test
    @StdIo
    void when_cloudWatchPlatform_then_shouldLogJsonToConsole(StdOut stdOut) {
        String logMessage = "Some message logged using cloudwatch profile to console";
        log.info(logMessage);

        Map<String, String> msg = awaitAndAssertOneJsonLogEntryOnStdOutContaining(logMessage, stdOut);

        assertEquals(logMessage, msg.get("message"));
        assertEquals("test-app", msg.get("app"));
        assertNotNull(msg.get("timestamp"), "Timestamp is logged: " + msg);
        assertNull(msg.get("logrelayHost"), "Context properties should not be logged");
    }
}
