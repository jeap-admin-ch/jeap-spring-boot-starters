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
import org.springframework.context.annotation.Configuration;
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

    /**
     * Servlet-specific beans are kept in a nested configuration so that this auto-configuration can be
     * introspected (and used) in non-web applications where the servlet API is absent. If the
     * {@code FilterRegistrationBean<UnhandledExceptionLoggingFilter>} bean method were declared directly
     * on {@link JeapLoggingAutoconfig}, reflecting its bean methods (which Spring's component scan does
     * unconditionally, before any {@code @ConditionalOnWebApplication} is evaluated) would fail with
     * {@code NoClassDefFoundError: jakarta/servlet/Filter} on a non-web classpath.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class ServletConfiguration {

        @Bean
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

}
