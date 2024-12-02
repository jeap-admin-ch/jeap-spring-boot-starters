package ch.admin.bit.jeap.monitor.metrics.app;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class SpringBootAppNameMetricsAutoConfig {

    @Bean
    SpringBootAppNameMetricsInitializer springBootAppNameMetricsInitializer(MeterRegistry meterRegistry) {
        return new SpringBootAppNameMetricsInitializer(meterRegistry);
    }
}
