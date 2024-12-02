package ch.admin.bit.jeap.monitor.metrics.security;

import ch.admin.bit.jeap.rest.tracing.security.RestResponseSecurityTrace;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestSecurityTracingMetricsTest {

    private static final String TAG_PRODUCER = "producer";
    private static final String TAG_DATAPOINT = "datapoint"; // URI
    private static final String TAG_METHOD = "method"; // HTTP method
    private static final String TAG_HTTP_STATUS = "status"; // Http status
    private static final String TAG_AUTH = "auth"; // Authentication class

    private static final String METRIC_NAME = "jeap_rest_endpoint_without_jwt";

    public static final String URI_PATTERN = "uriPattern";

    @Test
    void onResponse_validTrace_counterIncremented() {
        //given
        MeterRegistry meterRegistry = mock(MeterRegistry.class);

        RestSecurityTracingMetrics restSecurityTracingMetrics = new RestSecurityTracingMetrics(meterRegistry);

        RestResponseSecurityTrace trace1 = getRestResponseSecurityTrace(URI_PATTERN, 200);
        Counter counter1 = mock(Counter.class);
        when(meterRegistry.counter(METRIC_NAME, createTags(trace1, null))).thenReturn(counter1);

        RestResponseSecurityTrace trace2 = getRestResponseSecurityTrace(URI_PATTERN, 201);
        Counter counter2 = mock(Counter.class);
        when(meterRegistry.counter(METRIC_NAME, createTags(trace2, null))).thenReturn(counter2);

        SecurityContextHolder.clearContext();

        //when
        restSecurityTracingMetrics.onResponse(trace1);
        restSecurityTracingMetrics.onResponse(trace1);

        restSecurityTracingMetrics.onResponse(trace2);

        //then
        verify(meterRegistry, times(2)).counter(METRIC_NAME, createTags(trace1, null));
        verify(meterRegistry, times(1)).counter(METRIC_NAME, createTags(trace2, null));

        verify(counter1, times(2)).increment();
        verify(counter2, times(1)).increment();
    }

    @Test
    void onResponse_traceWithoutRequestUriPattern_counterNotIncremented() {

        //given
        MeterRegistry meterRegistry = mock(MeterRegistry.class);
        RestSecurityTracingMetrics restSecurityTracingMetrics = new RestSecurityTracingMetrics(meterRegistry);

        SecurityContextHolder.clearContext();

        //when
        restSecurityTracingMetrics.onResponse(getRestResponseSecurityTrace(null, 200));

        //then
        verify(meterRegistry, never()).counter(anyString(), any(Tags.class));

    }

    @Test
    void onResponse_usernamePasswordAuthenticationToken_counterIncremented() {
        doTestWithContext(UsernamePasswordAuthenticationToken.class);
    }

    @Test
    void onResponse_anonymousAuthenticationToken_counterIncremented() {
        doTestWithContext(AnonymousAuthenticationToken.class);
    }

    private void doTestWithContext(Class<? extends Authentication> authentication) {
        //given
        MeterRegistry meterRegistry = mock(MeterRegistry.class);

        RestSecurityTracingMetrics restSecurityTracingMetrics = new RestSecurityTracingMetrics(meterRegistry);

        RestResponseSecurityTrace trace1 = getRestResponseSecurityTrace(URI_PATTERN, 200);
        Counter counter1 = mock(Counter.class);
        when(meterRegistry.counter(METRIC_NAME, createTags(trace1, authentication.getSimpleName()))).thenReturn(counter1);

        SecurityContext securityContextMock = mock(SecurityContext.class);
        when(securityContextMock.getAuthentication()).thenReturn(mock(authentication));
        SecurityContextHolder.setContext(securityContextMock);

        //when
        restSecurityTracingMetrics.onResponse(trace1);

        //then
        verify(meterRegistry, times(1)).counter(METRIC_NAME, createTags(trace1, authentication.getSimpleName()));
        verify(counter1, times(1)).increment();
    }

    private Tags createTags(RestResponseSecurityTrace restResponseSecurityTrace, String auth) {
        return Tags.of(
                TAG_PRODUCER, "na",
                TAG_HTTP_STATUS, String.valueOf(restResponseSecurityTrace.statusCode()),
                TAG_DATAPOINT, URI_PATTERN,
                TAG_METHOD, HttpMethod.GET.name(),
                TAG_AUTH, auth == null ? "null" : auth);
    }

    private RestResponseSecurityTrace getRestResponseSecurityTrace(String uriPattern, int statusCode) {
        return new RestResponseSecurityTrace(HttpMethod.GET.name(), uriPattern, statusCode);
    }

}
