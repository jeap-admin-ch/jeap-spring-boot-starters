package ch.admin.bit.jeap.rest.tracing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Logs requests and responses for Mvc-Applications using technology neutral {@link RestRequestTracer}
 * <p>
 * It is similar to {@link org.springframework.web.filter.CommonsRequestLoggingFilter}
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequiredArgsConstructor
class ServletRequestTracer extends OncePerRequestFilter {
    /**
     * See org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE -
     * using a String attribute name here to avoid a dependency on spring-webmvc
     */
    private static final String BEST_MATCHING_PATTERN_ATTRIBUTE =
            "org.springframework.web.servlet.HandlerMapping.bestMatchingPattern";
    public static final String UNKNOWN_METHOD = "UNKNOWN";

    private final RestRequestTracer restRequestTracer;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        boolean shouldSkipTracing = isAsyncDispatch(request);
        if (shouldSkipTracing) {
            filterChain.doFilter(request, response);
            return;
        }
        ZonedDateTime incomingTime = ZonedDateTime.now();
        logRequest(request);
        try {
            filterChain.doFilter(request, response);
        } finally {
            logResponse(request, response, incomingTime);
        }
    }

    private void logRequest(HttpServletRequest request) {
        ServletServerHttpRequest servletServerHttpRequest = request instanceof ServletServerHttpRequest ?
                (ServletServerHttpRequest) request : new ServletServerHttpRequest(request);
        String method = method(servletServerHttpRequest);
        restRequestTracer.onRequestBuilder()
                .method(method)
                .requestUri(servletServerHttpRequest.getURI().toASCIIString())
                .protocol(request.getProtocol())
                .emit();
    }

    private void logResponse(HttpServletRequest request, HttpServletResponse response, ZonedDateTime incomingTime) {
        Map<String, Object> attributes = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(request.getAttributeNames().asIterator(), Spliterator.ORDERED), false)
                .collect(Collectors.toMap(Function.identity(), request::getAttribute));
        Map<String, List<String>> responseHeaders = response.getHeaderNames().stream().distinct()
                .collect(Collectors.toMap(Function.identity(), name -> new ArrayList<>(response.getHeaders(name))));
        ServletServerHttpRequest servletServerHttpRequest = request instanceof ServletServerHttpRequest ?
                (ServletServerHttpRequest) request : new ServletServerHttpRequest(request);
        String user = (String) Optional.ofNullable(request.getAttribute(ServletStoreUserFilter.USERNAME_ATTRIBUTE))
                .filter(value -> value instanceof String)
                .orElse(null);
        restRequestTracer.onResponseBuilder()
                .method(method(servletServerHttpRequest))
                .requestUri(servletServerHttpRequest.getURI().toASCIIString())
                .requestUriPattern((String) request.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE))
                .user(user)
                .incomingTime(incomingTime)
                .statusCode(response.getStatus())
                .remoteAddr(servletServerHttpRequest.getRemoteAddress())
                .responseHeaders(responseHeaders)
                .requestHeaders(servletServerHttpRequest.getHeaders())
                .attributes(attributes)
                .emit();
    }

    private static String method(ServletServerHttpRequest request) {
        return request.getMethod() == null ? UNKNOWN_METHOD : request.getMethod().toString();
    }
}
