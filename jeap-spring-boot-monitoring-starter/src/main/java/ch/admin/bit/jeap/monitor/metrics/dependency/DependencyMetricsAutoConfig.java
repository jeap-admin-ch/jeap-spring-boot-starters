package ch.admin.bit.jeap.monitor.metrics.dependency;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class DependencyMetricsAutoConfig {

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public DependencyMetricsInitializer dependencyMetricsInitializer(MeterRegistry meterRegistry) {
        return new DependencyMetricsInitializer(dependencyVersionProvider(), meterRegistry);
    }

    @Bean
    public DependencyVersionProvider dependencyVersionProvider() {
        return new DependencyVersionProvider();
    }

}
