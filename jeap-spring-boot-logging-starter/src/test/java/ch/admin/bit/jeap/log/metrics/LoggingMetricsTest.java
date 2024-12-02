package ch.admin.bit.jeap.log.metrics;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = LoggingMetrics.class)
class LoggingMetricsTest {

    @Test
    void ensureLoggingMetricsStartsWithoutMicrometerOnClasspath() {
        assertTrue(LoggingMetrics.staticLoggingMetricsProvider instanceof NopLoggingMetricsProvider);
        LoggingMetrics.incrementDistributedLogTransmitError();
    }
}
