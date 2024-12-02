package ch.admin.bit.jeap.log.metrics;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Records logging metrics using a {@link LoggingMetricsProvider} (using Micrometer if available, otherwise NOP)
 * <br>
 * Is initialized after the application has successfully started to avoid race conditions in auto config initialization
 * between micrometer and logging. Does not statically link to micrometer to avoid a hard dependency on micrometer.
 * <br>
 * Buffers counter increments locally until a metrics provider has been initialized to avoid losing counters when
 * micrometer is not yet initialized.
 */
public class LoggingMetrics implements DisposableBean {
    /**
     * Logback is initialized before the Spring context and therefore has no access to the Spring context. The lifecycle
     * of the appenders that provide metrics is controlled by Logback and not by Spring. Somewhere there needs to be a
     * connection between the two lifecycle containers, hence the static variable. An alternative would be to put the
     * spring bean in the logback context as a property after Spring initialization, which does however not provide any
     * benefit and requires much more code.
     */
    static LoggingMetricsProvider staticLoggingMetricsProvider = null;

    private static final AtomicInteger distributedLogConnectionEstablished = new AtomicInteger(0);
    private static final AtomicInteger distributedLogConnectionError = new AtomicInteger(0);
    private static final AtomicInteger distributedLogTransmitError = new AtomicInteger(0);

    @EventListener
    public void onApplicationStartedEvent(ApplicationStartedEvent event) {
        try {
            Class<?> meterRegistryType = Class.forName("io.micrometer.core.instrument.MeterRegistry");
            Object meterRegistry = event.getApplicationContext().getBean(meterRegistryType);
            // Micrometer is on the classpath, and a meter registry is available in the spring context
            staticLoggingMetricsProvider = new MicrometerLoggingMetricsProvider(meterRegistry); //NOSONAR
            flushCounters();
        } catch (Throwable t) {
            // Micrometer is not on the classpath, or no meter registry bean available -> don't provide logging metrics
            staticLoggingMetricsProvider = new NopLoggingMetricsProvider(); //NOSONAR
        }
    }

    /**
     * Flush any cached counter values that have been counted before the spring context has finished initialization.
     */
    private synchronized void flushCounters() {
        flushCounter(distributedLogConnectionEstablished, LoggingMetrics::incrementDistributedLogConnectionEstablished);
        flushCounter(distributedLogConnectionError, LoggingMetrics::incrementDistributedLogConnectionError);
        flushCounter(distributedLogTransmitError, LoggingMetrics::incrementDistributedLogTransmitError);
    }

    private static void flushCounter(AtomicInteger counter, Runnable action) {
        for (int i = 0; i < counter.getAndSet(0); i++) {
            action.run();
        }
    }

    @Override
    @SuppressWarnings("java:S2696")
    public void destroy() {
        staticLoggingMetricsProvider = null;
    }

    public static void incrementDistributedLogConnectionEstablished() {
        withMetricsProviderOrFallback(
                LoggingMetricsProvider::incrementDistributedLogConnectionEstablished,
                distributedLogConnectionEstablished);

    }

    public static void incrementDistributedLogConnectionError() {
        withMetricsProviderOrFallback(
                LoggingMetricsProvider::incrementDistributedLogConnectionError,
                distributedLogConnectionError);

    }

    public static void incrementDistributedLogTransmitError() {
        withMetricsProviderOrFallback(
                LoggingMetricsProvider::incrementDistributedLogTransmitError,
                distributedLogTransmitError);
    }

    public static void distributedLogTransmitTime(Duration duration) {
        LoggingMetricsProvider metricsProvider = staticLoggingMetricsProvider;
        if (metricsProvider != null) {
            metricsProvider.distributedLogTransmitTime(duration);
        }
    }

    /**
     * If a {@link LoggingMetricsProvider} is set, invoke a counter increment method on it. Otherwise, buffer the
     * counter increment in a local atomic integer until a metrics provider has been initialized.
     */
    private static void withMetricsProviderOrFallback(Consumer<LoggingMetricsProvider> withMetricsProvider, AtomicInteger fallback) {
        LoggingMetricsProvider metricsProvider = staticLoggingMetricsProvider;
        if (metricsProvider != null) {
            withMetricsProvider.accept(metricsProvider);
        } else {
            fallback.getAndIncrement();
        }
    }
}
