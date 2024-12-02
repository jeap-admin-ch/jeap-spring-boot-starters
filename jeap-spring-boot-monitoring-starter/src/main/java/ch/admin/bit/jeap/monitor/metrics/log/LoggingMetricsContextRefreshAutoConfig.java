package ch.admin.bit.jeap.monitor.metrics.log;


import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.TurboFilter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.metrics.LogbackMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = LogbackMetricsAutoConfiguration.class)
public class LoggingMetricsContextRefreshAutoConfig {

    @ConditionalOnBean({LogbackMetrics.class, MeterRegistry.class})
    @Bean
    MetricsRefreshEventListener metricsEventListener(final LogbackMetrics logbackMetrics, final MeterRegistry meterRegistry) {
        return new MetricsRefreshEventListener(logbackMetrics, meterRegistry);
    }

    /**
     * This is a workaround for the fact that the logger context will change when spring cloud refreshed the
     * application context, and the metrics filter from micrometer's LogbackMetrics will not be attached to the current
     * logger context. There are multiple open/closed issues in micrometer/springboot/springcloud, but is has
     * still not been fixed.
     * <p>
     * This workaround re-registers the metrics filter when the context has been refreshed, to make sure the logging
     * metric counters are still increased afterwards.
     * <a href="https://github.com/micrometer-metrics/micrometer/issues/564">Micrometer Issue 564</a>
     * <a href="https://github.com/spring-cloud/spring-cloud-commons/issues/608">Spring CLoud Issue 608</a>
     */
    static class MetricsRefreshEventListener implements ApplicationListener<RefreshScopeRefreshedEvent> {
        private final LogbackMetrics logbackMetrics;
        private final MeterRegistry meterRegistry;

        public MetricsRefreshEventListener(LogbackMetrics logbackMetrics, MeterRegistry meterRegistry) {
            this.logbackMetrics = logbackMetrics;
            this.meterRegistry = meterRegistry;
        }

        @Override
        public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

            boolean metricsTurboFilterRegistered = loggerContext.getTurboFilterList().stream()
                    .anyMatch(this::isTurboFilterRegistered);
            if (!metricsTurboFilterRegistered) {
                logbackMetrics.bindTo(meterRegistry);
            }
        }

        private boolean isTurboFilterRegistered(TurboFilter filter) {
            // MetricsTurboFilter is contained in LogbackMetrics, but not a public class.
            return filter != null && filter.getClass().getSimpleName().equals("MetricsTurboFilter");
        }
    }
}
