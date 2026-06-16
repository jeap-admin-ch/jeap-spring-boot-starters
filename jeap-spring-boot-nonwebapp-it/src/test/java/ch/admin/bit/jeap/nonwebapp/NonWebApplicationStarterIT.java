package ch.admin.bit.jeap.nonwebapp;

import ch.admin.bit.jeap.log.metrics.LoggingMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The jeap-logging-starter (and the monitoring starter) must also work in
 * non-web Spring applications.
 * <p>
 * This module deliberately has a reduced classpath: it depends on the starters like a real non-web
 * application would, without any web starter, so {@code jakarta.servlet.Filter} is genuinely absent.
 * <p>
 * Running the application context as a non-web app ({@code webEnvironment = NONE}) triggers
 * Spring Boot's component scan with its {@code TypeExcludeFilter}, which forces reflection over every
 * auto-configuration's {@code @Bean} factory methods - unconditionally, before any
 * {@code @ConditionalOnWebApplication} is evaluated. Before the fix this fails on
 * {@code JeapLoggingAutoconfig}, whose {@code @Bean} method returns
 * {@code FilterRegistrationBean<UnhandledExceptionLoggingFilter>} (a {@code jakarta.servlet.Filter}),
 * throwing {@code NoClassDefFoundError: jakarta/servlet/Filter}, wrapped into
 * {@code IllegalStateException: Failed to introspect Class [ch.admin.bit.jeap.log.JeapLoggingAutoconfig]}.
 * <p>
 * Beyond merely starting the context, the assertions below verify that the starters actually
 * function in this non-web setup.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ExtendWith(OutputCaptureExtension.class)
class NonWebApplicationStarterIT {

    private static final Logger log = LoggerFactory.getLogger(NonWebApplicationStarterIT.class);

    // A bean contributed by the logging starter - if the starter's auto-configuration did not run,
    // this injection would already fail.
    @Autowired
    private LoggingMetrics loggingMetrics;

    // Provided by the monitoring starter's metrics auto-configuration.
    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void servletApiIsNotOnTheClasspath() {
        // The whole point of this module: a genuine non-web classpath without the servlet API.
        assertThatThrownBy(() -> Class.forName("jakarta.servlet.Filter"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void loggingStarterBeanIsInjectable() {
        assertThat(loggingMetrics).isNotNull();
    }

    @Test
    void metricCanBeRecorded() {
        // The monitoring starter must provide a working metrics registry even without a web server.
        assertThat(meterRegistry).isNotNull();

        Counter counter = Counter.builder("nonweb_it_test_counter").register(meterRegistry);
        counter.increment();

        assertThat(meterRegistry.get("nonweb_it_test_counter").counter().count()).isEqualTo(1.0d);
    }

    @Test
    void loggingStarterMetricsAreWiredToTheRegistry() {
        // The logging starter records its metrics via Micrometer once the context has started. As
        // Micrometer and a MeterRegistry are available, incrementing must reach the registry.
        double before = meterRegistry.get("logging_distlog_transmit_error").counter().count();

        LoggingMetrics.incrementDistributedLogTransmitError();

        assertThat(meterRegistry.get("logging_distlog_transmit_error").counter().count())
                .isEqualTo(before + 1.0d);
    }

    @Test
    void loggingWorks(CapturedOutput output) {
        String message = "non-web logging smoke test marker";
        log.info(message);

        assertThat(output).contains(message);
    }
}
