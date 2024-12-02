package ch.admin.bit.jeap.log.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

class MicrometerLoggingMetricsProvider implements LoggingMetricsProvider {

    private static final String DIST_LOG_CONNECTION_ERROR = "logging_distlog_connection_error";
    private static final String DIST_LOG_CONNECTION_ESTABLISHED = "logging_distlog_connection_established";
    private static final String DIST_LOG_TRANSMIT_ERROR = "logging_distlog_transmit_error";
    private static final String DIST_LOG_TRANSMIT_TIME = "logging_distlog_transmit_time";

    private final Counter distLogConnectionError;
    private final Counter distLogConnectionEstablished;
    private final Counter distLogTransmitError;
    private final Timer logTransmitTimer;

    MicrometerLoggingMetricsProvider(Object meterRegistryBean) {
        MeterRegistry meterRegistry = (MeterRegistry) meterRegistryBean;
        // Failed to connect to distributed logging server
        distLogConnectionError = Counter.builder(DIST_LOG_CONNECTION_ERROR)
                .register(meterRegistry);
        // Connected to distributed log server
        distLogConnectionEstablished = Counter.builder(DIST_LOG_CONNECTION_ESTABLISHED)
                .register(meterRegistry);
        // Failed to transmit log entry to distributed logging server
        distLogTransmitError = Counter.builder(DIST_LOG_TRANSMIT_ERROR)
                .register(meterRegistry);
        logTransmitTimer = Timer.builder(DIST_LOG_TRANSMIT_TIME)
                .publishPercentiles(0.5, 0.95, 0.99)
                .distributionStatisticExpiry(Duration.ofHours(24))
                .register(meterRegistry);
    }

    @Override
    public void incrementDistributedLogConnectionEstablished() {
        distLogConnectionEstablished.increment();
    }

    @Override
    public void incrementDistributedLogConnectionError() {
        distLogConnectionError.increment();
    }

    @Override
    public void incrementDistributedLogTransmitError() {
        distLogTransmitError.increment();
    }

    @Override
    public void distributedLogTransmitTime(Duration duration) {
        logTransmitTimer.record(duration);
    }
}
