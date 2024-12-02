package ch.admin.bit.jeap.monitor.metrics.rest;

import ch.admin.bit.jeap.rest.tracing.RestResponseListener;
import ch.admin.bit.jeap.rest.tracing.RestResponseTrace;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;

@SuppressWarnings("NullableProblems")
@Slf4j
@RequiredArgsConstructor
public class RestTracingMetrics implements RestResponseListener {

    private static final String METRIC_NAME = "jeap_relation";

    private static final String TAG_PRODUCER = "producer"; // Spring boot app name
    private static final String TAG_CONSUMER = "consumer"; // Caller header from request
    private static final String TAG_DATAPOINT = "datapoint"; // URI
    private static final String TAG_TECHNOLOGY = "technology"; // http
    private static final String TAG_METHOD = "method"; // HTTP method

    private static final String TECHNOLOGY = "http";

    @Value("${jeap.monitor.metrics.rest.maximum-allowable-jeap-relation-metrics}")
    private int maximumAllowableJeapRelationMetrics = 10;

    @Value("${spring.application.name:na}")
    private String applicationName = "na";

    private final MeterRegistry meterRegistry;

    @PostConstruct
    void initialize() {
        meterRegistry.config().meterFilter(maximumAllowableJeapRelationMetrics(maximumAllowableJeapRelationMetrics));
    }

    @Override
    public void onResponse(RestResponseTrace restResponseTrace) {
        if (restResponseTrace.getCaller() != null && restResponseTrace.getRequestUriPattern() != null) {
            meterRegistry.counter(METRIC_NAME, getTags(restResponseTrace)).increment();
        }
    }

    private Tags getTags(RestResponseTrace restResponseTrace) {
        return Tags.of(
                TAG_PRODUCER, applicationName,
                TAG_CONSUMER, restResponseTrace.getCaller(),
                TAG_DATAPOINT, restResponseTrace.getRequestUriPattern(),
                TAG_TECHNOLOGY, TECHNOLOGY,
                TAG_METHOD, restResponseTrace.getMethod());
    }

    @Override
    public boolean isResponseListenerActive() {
        return true;
    }

    static MeterFilter maximumAllowableJeapRelationMetrics(int maximumTimeSeries) {
        return new MeterFilter() {
            private final MeterFilter delegate = MeterFilter.maximumAllowableMetrics(maximumTimeSeries);

            @Override
            public MeterFilterReply accept(Meter.Id id) {
                if (METRIC_NAME.equals(id.getName())) {
                    return delegate.accept(id);
                }

                return MeterFilterReply.NEUTRAL;
            }
        };
    }
}
