package ch.admin.bit.jeap.web.configuration.servlet;

import ch.admin.bit.jeap.web.configuration.HeaderConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        verify(servletHeaders).addHeaders(any(NoStoreOnErrorResponseWrapper.class), eq(method), eq("/index.html"));
        verify(filterChain).doFilter(eq(request), any(NoStoreOnErrorResponseWrapper.class));
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

        verify(servletHeaders).addHeaders(any(NoStoreOnErrorResponseWrapper.class),
                eq(HttpMethod.GET.name()), eq("/assets/logo.png"));
        verify(filterChain).doFilter(eq(request), any(NoStoreOnErrorResponseWrapper.class));
    }

    @Test
    void doFilterInternal_emptyServletPathAndNoPathInfo_usesRootPath() throws Exception {
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(request.getServletPath()).thenReturn("");
        when(request.getPathInfo()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(servletHeaders).addHeaders(any(NoStoreOnErrorResponseWrapper.class),
                eq(HttpMethod.GET.name()), eq("/"));
        verify(filterChain).doFilter(eq(request), any(NoStoreOnErrorResponseWrapper.class));
    }

    @Test
    void doFilterInternal_sendError_preventsCachingBeforeResponseIsCommitted() throws Exception {
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(request.getServletPath()).thenReturn("/old-bundle.js");
        doAnswer(invocation -> {
            HttpServletResponse filteredResponse = invocation.getArgument(1);
            filteredResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }).when(filterChain).doFilter(eq(request), any(NoStoreOnErrorResponseWrapper.class));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        verify(response).setHeader(HttpHeaders.EXPIRES, "0");
        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void doFilterInternal_errorStatus_preventsCaching() throws Exception {
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(request.getServletPath()).thenReturn("/old-bundle.js");
        doAnswer(invocation -> {
            HttpServletResponse filteredResponse = invocation.getArgument(1);
            filteredResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }).when(filterChain).doFilter(eq(request), any(NoStoreOnErrorResponseWrapper.class));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        verify(response).setHeader(HttpHeaders.EXPIRES, "0");
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void doFilterInternal_filterChainException_preventsCachingAndPropagatesException() throws Exception {
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(request.getServletPath()).thenReturn("/old-bundle.js");
        ServletException failure = new ServletException("request failed");
        doThrow(failure).when(filterChain).doFilter(eq(request), any(NoStoreOnErrorResponseWrapper.class));

        ServletException thrown = assertThrows(ServletException.class,
                () -> filter.doFilterInternal(request, response, filterChain));

        assertSame(failure, thrown);
        verify(response).setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        verify(response).setHeader(HttpHeaders.EXPIRES, "0");
    }

    @Test
    void doFilterInternal_exceptionDuringHeaderAddition_filterChainStillCalled() throws Exception {
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(request.getServletPath()).thenReturn("/index.html");
        when(request.getPathInfo()).thenReturn(null);
        doThrow(new RuntimeException("unexpected error"))
                .when(servletHeaders).addHeaders(any(NoStoreOnErrorResponseWrapper.class),
                        eq(HttpMethod.GET.name()), eq("/index.html"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(eq(request), any(NoStoreOnErrorResponseWrapper.class));
    }
}


