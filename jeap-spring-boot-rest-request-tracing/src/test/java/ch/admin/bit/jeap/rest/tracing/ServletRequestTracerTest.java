package ch.admin.bit.jeap.rest.tracing;

import jakarta.servlet.FilterChain;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServletRequestTracerTest {

    private static final String BEST_MATCHING_PATTERN_ATTRIBUTE =
            "org.springframework.web.servlet.HandlerMapping.bestMatchingPattern";

    @Mock
    private RestRequestListener restRequestListener;

    @Mock
    private RestResponseListener restResponseListener;

    @Captor
    private ArgumentCaptor<RestRequestTrace> requestTraceCaptor;

    @Captor
    private ArgumentCaptor<RestResponseTrace> responseTraceCaptor;

    private ServletRequestTracer servletRequestTracer;

    @BeforeEach
    void setUp() {
        lenient().when(restRequestListener.isRequestListenerActive()).thenReturn(true);
        lenient().when(restResponseListener.isResponseListenerActive()).thenReturn(true);

        TracerConfiguration tracerConfiguration = TracerConfiguration.builder()
                .headerBlacklist(List.of())
                .attributesWhitelist(List.of())
                .build();

        RestRequestTracer restRequestTracer = new RestRequestTracer(tracerConfiguration, List.of(restRequestListener), List.of(restResponseListener));
        servletRequestTracer = new ServletRequestTracer(restRequestTracer);
    }

    @Test
    @SneakyThrows
    void doFilterInternal_normalRequest_requestAndResponseListenersCalled() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restRequestListener).onRequest(requestTraceCaptor.capture());
        assertThat(requestTraceCaptor.getValue().getMethod()).isEqualTo("GET");
        assertThat(requestTraceCaptor.getValue().getRequestUri()).contains("/api/resource");

        verify(restResponseListener).onResponse(responseTraceCaptor.capture());
        assertThat(responseTraceCaptor.getValue().getMethod()).isEqualTo("GET");
        assertThat(responseTraceCaptor.getValue().getStatusCode()).isEqualTo(200);
    }

    @Test
    @SneakyThrows
    void doFilterInternal_normalRequest_filterChainIsCalled() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @SneakyThrows
    void doFilterInternal_asyncDispatch_listenersNotCalled() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        request.setDispatcherType(jakarta.servlet.DispatcherType.ASYNC);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restRequestListener, never()).onRequest(any());
        verify(restResponseListener, never()).onResponse(any());
    }

    @Test
    @SneakyThrows
    void doFilterInternal_asyncDispatch_filterChainIsCalled() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        request.setDispatcherType(jakarta.servlet.DispatcherType.ASYNC);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @SneakyThrows
    void doFilterInternal_requestUriPatternAttributePresent_responseTraceContainsUriPattern() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/resource/123");
        request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/resource/{id}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restResponseListener).onResponse(responseTraceCaptor.capture());
        assertThat(responseTraceCaptor.getValue().getRequestUriPattern()).isEqualTo("/api/resource/{id}");
    }

    @Test
    @SneakyThrows
    void doFilterInternal_noRequestUriPatternAttribute_responseTraceHasNullUriPattern() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restResponseListener).onResponse(responseTraceCaptor.capture());
        assertThat(responseTraceCaptor.getValue().getRequestUriPattern()).isNull();
    }

    @Test
    @SneakyThrows
    void doFilterInternal_userAttributePresent_responseTraceContainsUser() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        request.setAttribute(ServletStoreUserFilter.USERNAME_ATTRIBUTE, "john.doe");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restResponseListener).onResponse(responseTraceCaptor.capture());
        assertThat(responseTraceCaptor.getValue().getUser()).isEqualTo("john.doe");
    }

    @Test
    @SneakyThrows
    void doFilterInternal_noUserAttribute_responseTraceHasNullUser() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restResponseListener).onResponse(responseTraceCaptor.capture());
        assertThat(responseTraceCaptor.getValue().getUser()).isNull();
    }

    @Test
    @SneakyThrows
    void doFilterInternal_userAttributeIsNotAString_responseTraceHasNullUser() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        request.setAttribute(ServletStoreUserFilter.USERNAME_ATTRIBUTE, 42);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restResponseListener).onResponse(responseTraceCaptor.capture());
        assertThat(responseTraceCaptor.getValue().getUser()).isNull();
    }

    @Test
    @SneakyThrows
    void doFilterInternal_responseHeadersPresent_responseTraceContainsResponseHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.addHeader("Content-Type", "application/json");
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restResponseListener).onResponse(responseTraceCaptor.capture());
        assertThat(responseTraceCaptor.getValue().getResponseHeaders()).containsKey("Content-Type");
        assertThat(responseTraceCaptor.getValue().getResponseHeaders().get("Content-Type")).contains("application/json");
    }

    @Test
    @SneakyThrows
    void doFilterInternal_requestHeadersPresent_responseTraceContainsRequestHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        request.addHeader("Accept", "application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restResponseListener).onResponse(responseTraceCaptor.capture());
        assertThat(responseTraceCaptor.getValue().getRequestHeaders()).containsKey("Accept");
        assertThat(responseTraceCaptor.getValue().getRequestHeaders().get("Accept")).contains("application/json");
    }

    @Test
    @SneakyThrows
    void doFilterInternal_elapsedTimeIsTracked_responseTraceHasNonNegativeElapsedMs() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restResponseListener).onResponse(responseTraceCaptor.capture());
        assertThat(responseTraceCaptor.getValue().getElapsedMs()).isNotNegative();
    }

    @Test
    @SneakyThrows
    void doFilterInternal_filterChainThrowsException_responseListenerIsStillCalled() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doThrow(new RuntimeException("filter error")).when(chain).doFilter(any(), any());

        try {
            servletRequestTracer.doFilterInternal(request, response, chain);
        } catch (RuntimeException _) {
            // Ignore
        }

        verify(restResponseListener).onResponse(any());
    }

    @Test
    @SneakyThrows
    void doFilterInternal_requestTracedWithProtocol_requestTraceContainsProtocol() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        request.setProtocol("HTTP/1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restRequestListener).onRequest(requestTraceCaptor.capture());
        assertThat(requestTraceCaptor.getValue().getProtocol()).isEqualTo("HTTP/1.1");
    }

    @Test
    @SneakyThrows
    void doFilterInternal_blacklistedHeaderInResponse_headerNotIncludedInResponseTrace() {
        TracerConfiguration tracerConfiguration = TracerConfiguration.builder()
                .headerBlacklist(List.of("Authorization"))
                .attributesWhitelist(List.of())
                .build();
        RestRequestTracer restRequestTracer = new RestRequestTracer(tracerConfiguration, List.of(restRequestListener), List.of(restResponseListener));
        servletRequestTracer = new ServletRequestTracer(restRequestTracer);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.addHeader("Authorization", "Bearer secret");
        response.addHeader("Content-Type", "application/json");
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restResponseListener).onResponse(responseTraceCaptor.capture());
        assertThat(responseTraceCaptor.getValue().getResponseHeaders()).doesNotContainKey("Authorization");
        assertThat(responseTraceCaptor.getValue().getResponseHeaders()).containsKey("Content-Type");
    }

    @Test
    @SneakyThrows
    void doFilterInternal_whitelistedAttributePresent_responseTraceContainsAttribute() {
        TracerConfiguration tracerConfiguration = TracerConfiguration.builder()
                .headerBlacklist(List.of())
                .attributesWhitelist(List.of("myAttr"))
                .build();
        RestRequestTracer restRequestTracer = new RestRequestTracer(tracerConfiguration, List.of(restRequestListener), List.of(restResponseListener));
        servletRequestTracer = new ServletRequestTracer(restRequestTracer);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        request.setAttribute("myAttribute", "someValue");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restResponseListener).onResponse(responseTraceCaptor.capture());
        assertThat(responseTraceCaptor.getValue().getAttributes()).containsEntry("myAttribute", "someValue");
    }

    @Test
    @SneakyThrows
    void doFilterInternal_nonWhitelistedAttributePresent_responseTraceDoesNotContainAttribute() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        request.setAttribute("secretAttribute", "sensitiveValue");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restResponseListener).onResponse(responseTraceCaptor.capture());
        assertThat(responseTraceCaptor.getValue().getAttributes()).doesNotContainKey("secretAttribute");
    }

    @Test
    @SneakyThrows
    void doFilterInternal_callerHeaderPresent_responseTraceContainsCaller() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        request.addHeader(AddSenderSystemHeaderToRestClient.APPLICATION_NAME_HEADER, "calling-service");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restResponseListener).onResponse(responseTraceCaptor.capture());
        assertThat(responseTraceCaptor.getValue().getCaller()).isEqualTo("calling-service");
    }

    @Test
    @SneakyThrows
    void doFilterInternal_noCallerHeader_responseTraceHasNullCaller() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restResponseListener).onResponse(responseTraceCaptor.capture());
        assertThat(responseTraceCaptor.getValue().getCaller()).isNull();
    }

    @Test
    @SneakyThrows
    void doFilterInternal_maskedHeader_headerValueReplacedWithAsterisks() {
        TracerConfiguration tracerConfiguration = TracerConfiguration.builder()
                .headerBlacklist(List.of())
                .headerMasked(List.of("X-Secret"))
                .attributesWhitelist(List.of())
                .build();
        RestRequestTracer restRequestTracer = new RestRequestTracer(tracerConfiguration, List.of(restRequestListener), List.of(restResponseListener));
        servletRequestTracer = new ServletRequestTracer(restRequestTracer);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        request.addHeader("X-Secret", "my-secret-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restResponseListener).onResponse(responseTraceCaptor.capture());
        assertThat(responseTraceCaptor.getValue().getRequestHeaders().get("X-Secret")).containsOnly("***");
    }

    @Test
    @SneakyThrows
    void doFilterInternal_noActiveRequestListeners_requestListenerNotCalled() {
        when(restRequestListener.isRequestListenerActive()).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restRequestListener, never()).onRequest(any());
    }

    @Test
    @SneakyThrows
    void doFilterInternal_noActiveResponseListeners_responseListenerNotCalled() {
        when(restResponseListener.isResponseListenerActive()).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resource");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        servletRequestTracer.doFilterInternal(request, response, chain);

        verify(restResponseListener, never()).onResponse(any());
    }
}

