package ch.admin.bit.jeap.monitor.metrics.health;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfiguration;

@AutoConfiguration
@RequiredArgsConstructor
public class HealthMetricsAutoConfig implements InitializingBean {

    private final MeterRegistry meterRegistry;
    private final HealthEndpoint healthEndpoint;

    @Override
    public void afterPropertiesSet() {
        this.meterRegistry.gauge("health", healthEndpoint, HealthMetricsAutoConfig::healthToCode);
    }

    private static int healthToCode(HealthEndpoint ep) {
        Status status = ep.health().getStatus();
        return status.equals(Status.UP) ? 1 : 0;
    }

}
