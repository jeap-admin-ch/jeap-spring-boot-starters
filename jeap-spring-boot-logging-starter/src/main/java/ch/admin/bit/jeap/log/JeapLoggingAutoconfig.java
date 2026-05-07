package ch.admin.bit.jeap.log;

import ch.admin.bit.jeap.log.metrics.LoggingMetrics;
import ch.admin.bit.jeap.log.rest.RestRequestLogger;
import ch.admin.bit.jeap.log.rest.UnhandledExceptionLoggingFilter;
import ch.admin.bit.jeap.rest.tracing.TracerConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

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

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(name = "jeap.logging.rest.unhandled-exception-logging.enabled", havingValue = "true")
    public FilterRegistrationBean<UnhandledExceptionLoggingFilter> unhandledExceptionLoggingFilter() {
        FilterRegistrationBean<UnhandledExceptionLoggingFilter> registration =
                new FilterRegistrationBean<>(new UnhandledExceptionLoggingFilter());
        // Just inside Spring Boot's ServerHttpObservationFilter (HIGHEST_PRECEDENCE + 1) so the trace
        // context (traceId, spanId) is still populated in the MDC when we log, but outside Spring
        // Security's filter chain and Spring Boot's ErrorPageFilter so exceptions they handle
        // themselves (e.g. AccessDeniedException, error-page forwards) are not double-logged here.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registration;
    }

}
