package ch.admin.bit.jeap.monitor.metrics.truststore;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

@AutoConfiguration
@Conditional(TrustStoreMetricsCondition.class)
public class TrustStoreMetricsAutoConfig {

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public TrustStoreMetricsInitializer trustStoreMetricsInitializer(MeterRegistry meterRegistry) {
        return new TrustStoreMetricsInitializer(trustStoreService(), meterRegistry);
    }

    @Bean
    public TrustStoreService trustStoreService() {
        return new TrustStoreService();
    }

}
