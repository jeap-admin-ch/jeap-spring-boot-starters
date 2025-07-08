package ch.admin.bit.jeap.rest.tracing.security;

import ch.admin.bit.jeap.rest.tracing.TracerConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Trace requests after security context initialization in order to check the authentication.
 * Order of this configuration has to be the lowest (by default).
 * <p>
 * It is similar to {@link org.springframework.web.filter.CommonsRequestLoggingFilter}
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Slf4j
class ServletRequestSecurityTracer extends OncePerRequestFilter {
    /**
     * See org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE -
     * using a String attribute name here to avoid a dependency on spring-webmvc
     */
    private static final String BEST_MATCHING_PATTERN_ATTRIBUTE = "org.springframework.web.servlet.HandlerMapping.bestMatchingPattern";

    private final Pattern filterPattern;
    private final Optional<RestSecurityResponseListener> restSecurityResponseListener;

    public ServletRequestSecurityTracer(TracerConfiguration tracerConfiguration, Optional<RestSecurityResponseListener> restSecurityResponseListener) {
        this.filterPattern = tracerConfiguration.getUriFilterPattern();
        this.restSecurityResponseListener = restSecurityResponseListener;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        boolean shouldSkipTracing = isAsyncDispatch(request);
        if (shouldSkipTracing) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            String requestPathPattern = getRequestPathPattern(request);
            traceRequest(request.getMethod(), requestPathPattern, response.getStatus());
        }
    }

    private String getRequestPathPattern(HttpServletRequest request) {
        if (request.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE) instanceof String bestMatchingPattern &&
                StringUtils.hasText(bestMatchingPattern)) {
            // REST endpoint with known matching pattern called
            return bestMatchingPattern;
        }
        // (possibly) not a REST request (e.g. a SOAP request) and therefore matching pattern unknown
        // -> get the path from the request URI instead
        String fullPath = request.getRequestURI();
        String contextPath = request.getContextPath();
        if ((fullPath != null) && (contextPath != null)) {
            if (fullPath.startsWith(contextPath)) {
                return fullPath.substring(contextPath.length()); // strip context from request URI
            } else {
                log.warn("Expected request uri '{}' to start with context '{}'.", fullPath, contextPath);
                return null;
            }
        } else {
            log.warn("Unable to determine a request path pattern from request uri '{}' and request context '{}'.",
                    fullPath, contextPath);
            return null;
        }
    }

    private void traceRequest(String method, String requestUriPattern, int statusCode) {
        if (restSecurityResponseListener.isEmpty()) {
            return;
        }
        try {
            if (shouldTraceUri(requestUriPattern) && statusCode != 401 && statusCode != 403) {
                restSecurityResponseListener.get().onResponse(new RestResponseSecurityTrace(method, requestUriPattern, statusCode));
            }
        } catch (Exception ex) {
            log.warn("Exception in request listener", ex);
        }
    }

    private boolean shouldTraceUri(String uri) {
        if (!StringUtils.hasText(uri)) {
            log.warn("Unable to identify the request URI.");
            return false;
        }
        if (filterPattern == null) {
            return true;
        }
        return !filterPattern.matcher(uri).matches();
    }
}
