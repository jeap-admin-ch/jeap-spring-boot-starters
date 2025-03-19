package ch.admin.bit.jeap.monitor.metrics.health;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;

@AutoConfiguration
@RequiredArgsConstructor
@EnableScheduling
public class HealthMetricsAutoConfig {

    private final MeterRegistry meterRegistry;
    private final HealthEndpoint healthEndpoint;

    private Status currentStatus = Status.UP;

    @PostConstruct
    public void doSomethingAfterStartup() {
        this.meterRegistry.gauge("health", this, HealthMetricsAutoConfig::statusToCode);
    }

    private static int statusToCode(HealthMetricsAutoConfig instance) {
        return Status.UP.equals(instance.currentStatus) ? 1 : 0;
    }

    @Scheduled(fixedRateString = "${jeap.health.metric.update-rate-seconds:120}", timeUnit = TimeUnit.SECONDS)
    public void updateHealthMetrics() {
        currentStatus = healthEndpoint.health().getStatus();
    }
}
