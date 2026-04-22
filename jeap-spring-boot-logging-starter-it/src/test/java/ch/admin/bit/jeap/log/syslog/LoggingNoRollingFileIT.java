package ch.admin.bit.jeap.log.syslog;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Slf4j
@AutoConfigureMetrics
@ActiveProfiles("aws")
class LoggingNoRollingFileIT extends LogIntegrationTestBase {

    private static final Path LOGFILE_PATH = Path.of("log.log");

    @Test
    @StdIo
    void when_rollingFileIsSetToFalseInAwsProfile_then_shouldLogToFileAndStdout(StdOut stdOut) {
        String logMessage = "Some message logged using aws profile to console and not to the file";
        log.info(logMessage);

        awaitAndAssertOneLogEntryOnStdOutContaining(logMessage, stdOut);

        assertFalse(Files.exists(LOGFILE_PATH));
    }
}
