package ch.admin.bit.jeap.web.configuration.servlet;

import ch.admin.bit.jeap.web.configuration.HeaderConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

import java.util.stream.Stream;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddHeadersFilterTest {

    @Mock
    private ServletHeaders servletHeaders;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private AddHeadersFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AddHeadersFilter(new HeaderConfiguration(), servletHeaders);
    }

    @ParameterizedTest
    @MethodSource("provideHttpMethods")
    void doFilterInternal_requestForAcceptedPath_addsHeaders(String method) throws Exception {
        when(request.getMethod()).thenReturn(method);
        when(request.getServletPath()).thenReturn("/index.html");
        when(request.getPathInfo()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(servletHeaders).addHeaders(response, method, "/index.html");
        verify(filterChain).doFilter(request, response);
    }

    private static Stream<Arguments> provideHttpMethods() {
        return Stream.of(
                Arguments.of(HttpMethod.GET.name()),
                Arguments.of(HttpMethod.HEAD.name())
        );
    }

    @Test
    void doFilterInternal_postRequest_doesNotAddHeaders() throws Exception {
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());

        filter.doFilterInternal(request, response, filterChain);

        verify(servletHeaders, never()).addHeaders(response, HttpMethod.POST.name(), "/index.html");
        verify(filterChain).doFilter(request, response);
    }

    @ParameterizedTest
    @CsvSource({
            "/api,",
            "/api, /resource",
            "/some-consumer-api,"
    })
    void doFilterInternal_getRequestForSkippedApiPath_doesNotAddHeaders(String servletPath, String pathInfo) throws Exception {
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(request.getServletPath()).thenReturn(servletPath);
        when(request.getPathInfo()).thenReturn(pathInfo);

        filter.doFilterInternal(request, response, filterChain);

        verify(servletHeaders, never()).addHeaders(any(), any(), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_pathInfoAppendedToServletPath() throws Exception {
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(request.getServletPath()).thenReturn("/assets");
        when(request.getPathInfo()).thenReturn("/logo.png");

        filter.doFilterInternal(request, response, filterChain);

        verify(servletHeaders).addHeaders(response, HttpMethod.GET.name(), "/assets/logo.png");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_emptyServletPathAndNoPathInfo_usesRootPath() throws Exception {
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(request.getServletPath()).thenReturn("");
        when(request.getPathInfo()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(servletHeaders).addHeaders(response, HttpMethod.GET.name(), "/");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_exceptionDuringHeaderAddition_filterChainStillCalled() throws Exception {
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(request.getServletPath()).thenReturn("/index.html");
        when(request.getPathInfo()).thenReturn(null);
        doThrow(new RuntimeException("unexpected error"))
                .when(servletHeaders).addHeaders(response, HttpMethod.GET.name(), "/index.html");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}



