package ch.admin.bit.jeap.monitor.metrics.security;

import ch.admin.bit.jeap.rest.tracing.security.RestResponseSecurityTrace;
import ch.admin.bit.jeap.rest.tracing.security.RestSecurityResponseListener;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@RequiredArgsConstructor
public class RestSecurityTracingMetrics implements RestSecurityResponseListener {

    private static final String METRIC_NAME = "jeap_rest_endpoint_without_jwt";
    private static final String TAG_PRODUCER = "producer"; // Spring boot app name
    private static final String TAG_DATAPOINT = "datapoint"; // URI
    private static final String TAG_METHOD = "method"; // HTTP method
    private static final String TAG_HTTP_STATUS = "status"; // Http status
    private static final String TAG_AUTH = "auth"; // Authentication class

    private static final String JEAP_AUTHENTICATION_TOKEN = "JeapAuthenticationToken";

    @Value("${jeap.monitor.metrics.security.maximum-allowable-metrics}")
    private int maximumAllowableMetrics = 1000;

    @Value("${spring.application.name:na}")
    private String applicationName = "na";

    private final MeterRegistry meterRegistry;

    @PostConstruct
    void initialize() {
        meterRegistry.config().meterFilter(maximumAllowableMetrics(maximumAllowableMetrics));
    }

    @Override
    public void onResponse(RestResponseSecurityTrace restResponseTrace) {

        SecurityContext securityContext = SecurityContextHolder.getContext();
        String auth = null;

        if (securityContext != null && securityContext.getAuthentication() != null) {
            auth = securityContext.getAuthentication().getClass().getSimpleName();
        }

        if (restResponseTrace.requestUriPattern() != null && !JEAP_AUTHENTICATION_TOKEN.equals(auth)) {
            log.trace("No JeapAuthenticationToken token found in securityContext for request on '{}'", restResponseTrace.requestUriPattern());
            meterRegistry.counter(METRIC_NAME, getTags(restResponseTrace, auth)).increment();
        }
    }

    private Tags getTags(RestResponseSecurityTrace restResponseSecurityTrace, String auth) {
        return Tags.of(
                TAG_PRODUCER, applicationName,
                TAG_DATAPOINT, restResponseSecurityTrace.requestUriPattern(),
                TAG_METHOD, restResponseSecurityTrace.method(),
                TAG_HTTP_STATUS, String.valueOf(restResponseSecurityTrace.statusCode()),
                TAG_AUTH, auth == null ? "null" : auth);
    }

    static MeterFilter maximumAllowableMetrics(int maximumTimeSeries) {
        return new MeterFilter() {
            private final MeterFilter delegate = MeterFilter.maximumAllowableMetrics(maximumTimeSeries);

            @Override
            public @NonNull MeterFilterReply accept(@NonNull Meter.Id id) {
                if (METRIC_NAME.equals(id.getName())) {
                    return delegate.accept(id);
                }
                return MeterFilterReply.NEUTRAL;
            }
        };
    }
}
