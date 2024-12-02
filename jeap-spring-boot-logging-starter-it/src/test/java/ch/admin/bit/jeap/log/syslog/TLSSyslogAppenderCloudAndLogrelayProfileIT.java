package ch.admin.bit.jeap.log.syslog;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static java.util.Arrays.stream;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles({"logrelay", "cloud"})
@Slf4j
public class TLSSyslogAppenderCloudAndLogrelayProfileIT extends SyslogIntegrationTestBase {

    @Test
    @StdIo
    void when_logrelayAndCloudProfileAreActive_then_shouldOnlyLogToSyslog(StdOut stdOut) {
        String loggedMessage = "Some message";
        log.info(loggedMessage);

        Map<String, Object> logEntry = awaitAndAssertOneJsonLogEntryOnSyslogContaining(loggedMessage);
        Map<String, String> msg = getMsg(logEntry);
        assertEquals(loggedMessage, msg.get("message"));
        assertEquals("test-app", msg.get("app"));
        assertNull(msg.get("logrelayHost"), "Context properties should not be logged");

        assertTrue(stream(stdOut.capturedLines()).noneMatch(line -> line.contains(loggedMessage)), "should not log to console");
    }
}
