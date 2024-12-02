package ch.admin.bit.jeap.monitor.metrics.rest;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class RestTracingMetricsAutoConfig {

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public RestTracingMetrics testTracingMetrics(MeterRegistry meterRegistry) {
        return new RestTracingMetrics(meterRegistry);
    }

}
