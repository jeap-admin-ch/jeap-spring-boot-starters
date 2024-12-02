package ch.admin.bit.jeap.log.metrics;

import java.time.Duration;

interface LoggingMetricsProvider {

    void incrementDistributedLogConnectionEstablished();

    void incrementDistributedLogConnectionError();

    void incrementDistributedLogTransmitError();

    void distributedLogTransmitTime(Duration duration);
}
