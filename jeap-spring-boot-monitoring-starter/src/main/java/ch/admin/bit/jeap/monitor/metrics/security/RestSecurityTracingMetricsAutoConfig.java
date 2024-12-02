package ch.admin.bit.jeap.monitor.metrics.security;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class RestSecurityTracingMetricsAutoConfig {
    @Bean
    public RestSecurityTracingMetrics restSecurityTracingMetrics(MeterRegistry meterRegistry) {
        return new RestSecurityTracingMetrics(meterRegistry);
    }
}