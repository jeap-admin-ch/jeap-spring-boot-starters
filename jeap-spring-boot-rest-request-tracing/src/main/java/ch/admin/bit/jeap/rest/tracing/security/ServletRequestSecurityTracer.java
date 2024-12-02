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
            traceRequest(request.getMethod(), (String) request.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE), response.getStatus());
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
        if (filterPattern == null) {
            return true;
        }
        return !filterPattern.matcher(uri).matches();
    }
}
