package ch.admin.bit.jeap.log.syslog;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("logrelay")
@Slf4j
public class TLSSyslogAppenderIT extends SyslogIntegrationTestBase {

    @Test
    void when_messageIsLogged_then_shouldBeReceivedOnce() {
        String loggedMessage = "Some message";
        log.info(loggedMessage);
        String loggedErrorMessage = "Some error message";
        log.error(loggedErrorMessage);

        Map<String, Object> logEntry = awaitAndAssertOneJsonLogEntryOnSyslogContaining(loggedMessage);
        Map<String, String> message = getMsg(logEntry);
        assertEquals("INFO", message.get("level"));
        assertEquals(loggedMessage, message.get("message"));
        assertTrue(message.get("logger").contains(getClass().getSimpleName()));
        assertEquals(Thread.currentThread().getName(), message.get("thread_name"));
        assertNotNull(message.get("@timestamp"));

        Map<String, String> errorLogEntry = getMsg(awaitAndAssertOneJsonLogEntryOnSyslogContaining(loggedErrorMessage));
        assertEquals("ERROR", errorLogEntry.get("level"));
    }
}
