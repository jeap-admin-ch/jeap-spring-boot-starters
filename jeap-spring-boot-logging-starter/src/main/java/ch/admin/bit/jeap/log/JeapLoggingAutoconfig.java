package ch.admin.bit.jeap.log;

import ch.admin.bit.jeap.log.metrics.LoggingMetrics;
import ch.admin.bit.jeap.log.rest.RestRequestLogger;
import ch.admin.bit.jeap.rest.tracing.TracerConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class JeapLoggingAutoconfig {

    @Bean
    @ConditionalOnWebApplication // must match the condition on TracerConfiguration
    public RestRequestLogger restRequestLogger(TracerConfiguration tracerConfiguration) {
        return new RestRequestLogger(tracerConfiguration);
    }

    @Bean
    public LoggingMetrics loggingMetrics() {
        return new LoggingMetrics();
    }

}
