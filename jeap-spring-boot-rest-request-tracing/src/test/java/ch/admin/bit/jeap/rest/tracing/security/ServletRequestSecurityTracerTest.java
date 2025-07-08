package ch.admin.bit.jeap.rest.tracing.security;

import ch.admin.bit.jeap.rest.tracing.TracerConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
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

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ParameterizedTest()
    @ValueSource(booleans = {true, false})
    @interface BooleanValuesParametrizedTest {
    }

    private static final String BEST_MATCHING_PATTERN_ATTRIBUTE_NAME = "org.springframework.web.servlet.HandlerMapping.bestMatchingPattern";
    private static final String CONTEXT = "/test-context";

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

    @BooleanValuesParametrizedTest
    void doFilterInternal_requestShouldBeTraced_restSecurityResponseListenerCalled(boolean isRestRequest) {
        doFilterInternal("/api/foo/bar", isRestRequest, 200, true);
    }

    @BooleanValuesParametrizedTest
    void doFilterInternal_actuatorRequestShouldNotBeTraced_restSecurityResponseListenerNotCalled(boolean isRestRequest) {
        doFilterInternal("/actuator/info", isRestRequest, 200, false);
    }

    @BooleanValuesParametrizedTest
    void doFilterInternal_forbiddenRequestShouldNotBeTraced_restSecurityResponseListenerNotCalled(boolean isRestRequest) {
        doFilterInternal("/api/info", isRestRequest, 403, false);
    }

    @BooleanValuesParametrizedTest
    void doFilterInternal_unauthorizedRequestShouldNotBeTraced_restSecurityResponseListenerNotCalled(boolean isRestRequest) {
        doFilterInternal("/api/info", isRestRequest, 401, false);
    }

    @BooleanValuesParametrizedTest
    void doFilterInternal_missingRequestUri_restSecurityResponseListenerNotCalled(boolean isRestRequest) {
        doFilterInternal(null, isRestRequest, 200, false);
    }

    @SneakyThrows
    private void doFilterInternal(String requestUriPattern, boolean isRestRequest, int statusCode, boolean traceRequest) {
        String method = "POST";
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        HttpServletResponse responseMock = mock(HttpServletResponse.class);
        when(requestMock.getMethod()).thenReturn(method);
        if (isRestRequest && (requestUriPattern != null)) {
            when(requestMock.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE_NAME)).thenReturn(requestUriPattern);
        } else {
            when(requestMock.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE_NAME)).thenReturn(null);
            when(requestMock.getContextPath()).thenReturn(CONTEXT);
            String requestPath = (requestUriPattern != null ? CONTEXT + requestUriPattern : null);
            when(requestMock.getRequestURI()).thenReturn(requestPath);
        }
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
