package ch.admin.bit.jeap.rest.tracing.security;

import ch.admin.bit.jeap.rest.tracing.TracerConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServletRequestSecurityTracerTest {

    @Mock
    RestSecurityResponseListener restSecurityResponseListener;

    @Captor
    ArgumentCaptor<RestResponseSecurityTrace> restResponseSecurityTraceCaptor;

    ServletRequestSecurityTracer servletRequestSecurityTracer;

    @BeforeEach
    void setUp() {
        TracerConfiguration tracerConfiguration = TracerConfiguration.builder()
                .attributesWhitelist(List.of("TEST"))
                .headerBlacklist(List.of())
                .uriFilterPattern(Pattern.compile(".*/actuator/.*"))
                .build();

        servletRequestSecurityTracer = new ServletRequestSecurityTracer(tracerConfiguration, Optional.of(restSecurityResponseListener));
    }

    @Test
    void doFilterInternal_requestShouldBeTraced_restSecurityResponseListenerCalled() {
        doFilterInternal("/api/foo/bar", 200, true);
    }

    @Test
    void doFilterInternal_actuatorRequestShouldNotBeTraced_restSecurityResponseListenerNotCalled() {
        doFilterInternal("/actuator/info", 200, false);
    }

    @Test
    void doFilterInternal_forbiddenRequestShouldNotBeTraced_restSecurityResponseListenerNotCalled() {
        doFilterInternal("/api/info", 403, false);
    }

    @Test
    void doFilterInternal_unauthorizedRequestShouldNotBeTraced_restSecurityResponseListenerNotCalled() {
        doFilterInternal("/api/info", 401, false);
    }

    @SneakyThrows
    private void doFilterInternal(String requestUriPattern, int statusCode, boolean traceRequest) {
        String method = "POST";
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        HttpServletResponse responseMock = mock(HttpServletResponse.class);
        when(requestMock.getMethod()).thenReturn(method);
        when(requestMock.getAttribute("org.springframework.web.servlet.HandlerMapping.bestMatchingPattern")).thenReturn(requestUriPattern);
        when(responseMock.getStatus()).thenReturn(statusCode);
        servletRequestSecurityTracer.doFilterInternal(requestMock, responseMock, mock(FilterChain.class));

        if (traceRequest) {
            verify(restSecurityResponseListener).onResponse(restResponseSecurityTraceCaptor.capture());
            RestResponseSecurityTrace restResponseSecurityTrace = restResponseSecurityTraceCaptor.getValue();
            assertThat(restResponseSecurityTrace.requestUriPattern()).isEqualTo(requestUriPattern);
            assertThat(restResponseSecurityTrace.method()).isEqualTo(method);
            assertThat(restResponseSecurityTrace.statusCode()).isEqualTo(200);
        } else {
            verify(restSecurityResponseListener, never()).onResponse(any(RestResponseSecurityTrace.class));
        }

    }

}
