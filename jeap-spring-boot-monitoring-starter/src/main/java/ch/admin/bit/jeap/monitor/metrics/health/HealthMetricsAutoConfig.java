package ch.admin.bit.jeap.monitor.metrics.health;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@AutoConfiguration
@RequiredArgsConstructor
@EnableScheduling
public class HealthMetricsAutoConfig {

    private final MeterRegistry meterRegistry;
    private final HealthContributorRegistry healthContributorRegistry;
    private final HealthEndpoint healthEndpoint;

    private Status currentStatus = Status.UP;
    private final Map<String, Double> indicatorStatus = new ConcurrentHashMap<>();

    @Value("${jeap.health.metric.contributor-metrics.enabled:false}")
    private boolean perContributorMetricsEnabled;

    @PostConstruct
    public void createHealthMetric() {
        this.meterRegistry.gauge("health", this, HealthMetricsAutoConfig::statusToCode);

        if (perContributorMetricsEnabled) {
            healthContributorRegistry.stream().forEach(contributor -> {
                String name = contributor.getName();
                Gauge.builder("health_indicator_status", indicatorStatus, m -> m.getOrDefault(name, 0.0))
                        .tags(Tags.of("component", name))
                        .register(meterRegistry);
            });
        }
    }

    private static int statusToCode(HealthMetricsAutoConfig instance) {
        return Status.UP.equals(instance.currentStatus) ? 1 : 0;
    }

    @Scheduled(fixedRateString = "${jeap.health.metric.update-rate-seconds:120}", timeUnit = TimeUnit.SECONDS)
    public void updateHealthMetrics() {
        currentStatus = healthEndpoint.health().getStatus();

        if (perContributorMetricsEnabled) {
            healthContributorRegistry.stream().forEach(namedContributor -> {
                String name = namedContributor.getName();
                Object contributor = namedContributor.getContributor();
                if (contributor instanceof HealthIndicator indicator) {
                    try {
                        var status = indicator.health().getStatus();
                        indicatorStatus.put(name, Status.UP.equals(status) ? 1.0 : 0.0);
                    } catch (Exception e) {
                        indicatorStatus.put(name, 0.0);
                    }
                }
            });
        }
    }
}