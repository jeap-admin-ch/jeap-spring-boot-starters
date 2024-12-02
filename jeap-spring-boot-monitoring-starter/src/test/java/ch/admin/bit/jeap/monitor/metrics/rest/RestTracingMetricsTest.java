package ch.admin.bit.jeap.monitor.metrics.rest;

import ch.admin.bit.jeap.rest.tracing.RestResponseTrace;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestTracingMetricsTest {

    private static final String TAG_PRODUCER = "producer";
    private static final String TAG_CONSUMER = "consumer"; // Caller header from request
    private static final String TAG_DATAPOINT = "datapoint"; // URI
    private static final String TAG_TECHNOLOGY = "technology"; // http
    private static final String TAG_METHOD = "method"; // HTTP method

    private static final String METRIC_NAME = "jeap_relation";

    private static final String TECHNOLOGY = "http";
    public static final String URI_PATTERN = "uriPattern";

    @Test
    void onResponse_validTrace_counterIncremented(){
        //given
        MeterRegistry meterRegistry = mock(MeterRegistry.class);

        RestTracingMetrics restTracingMetrics = new RestTracingMetrics(meterRegistry);

        RestResponseTrace caller1 = getRestResponseTrace("caller1", URI_PATTERN);
        Counter counter1 = mock(Counter.class);
        when(meterRegistry.counter(METRIC_NAME, createTags(caller1))).thenReturn(counter1);

        RestResponseTrace caller2 = getRestResponseTrace("caller2", URI_PATTERN);
        Counter counter2 = mock(Counter.class);
        when(meterRegistry.counter(METRIC_NAME, createTags(caller2))).thenReturn(counter2);

        //when
        restTracingMetrics.onResponse(caller1);
        restTracingMetrics.onResponse(caller1);

        restTracingMetrics.onResponse(caller2);

        //then
        verify(meterRegistry,  times(2)).counter(METRIC_NAME, createTags(caller1));
        verify(meterRegistry,  times(1)).counter(METRIC_NAME, createTags(caller2));

        verify(counter1,  times(2)).increment();
        verify(counter2,  times(1)).increment();
    }

    @Test
    void onResponse_traceWithoutCaller_counterNotIncremented(){

        //given
        MeterRegistry meterRegistry = mock(MeterRegistry.class);
        RestTracingMetrics restTracingMetrics = new RestTracingMetrics(meterRegistry);

        //when
        restTracingMetrics.onResponse(getRestResponseTrace(null, URI_PATTERN));

        //then
        verify(meterRegistry,  never()).counter(anyString(), any(Tags.class));

    }

    @Test
    void onResponse_traceWithoutRequestUriPattern_counterNotIncremented(){

        //given
        MeterRegistry meterRegistry = mock(MeterRegistry.class);
        RestTracingMetrics restTracingMetrics = new RestTracingMetrics(meterRegistry);

        //when
        restTracingMetrics.onResponse(getRestResponseTrace("caller1", null));

        //then
        verify(meterRegistry,  never()).counter(anyString(), any(Tags.class));

    }

    private Tags createTags(RestResponseTrace restResponseTrace) {
        return Tags.of(
                TAG_PRODUCER, "na",
                TAG_CONSUMER, restResponseTrace.getCaller(),
                TAG_DATAPOINT, URI_PATTERN,
                TAG_TECHNOLOGY, TECHNOLOGY,
                TAG_METHOD, HttpMethod.GET.name());
    }

    private RestResponseTrace getRestResponseTrace(String caller, String uriPattern) {
        return RestResponseTrace.builder()
                .method(HttpMethod.GET.name())
                .caller(caller)
                .requestUri("uri")
                .requestUriPattern(uriPattern)
                .build();
    }

}
