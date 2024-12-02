package ch.admin.bit.jeap.featureflag.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
class MeterRegistryConfig {

    @Bean
    public MeterRegistry registry() {
        return new SimpleMeterRegistry();
    }

}
