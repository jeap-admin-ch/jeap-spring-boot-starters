package ch.admin.bit.jeap.web.configuration.servlet;

import ch.admin.bit.jeap.web.configuration.HeaderConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * A filter adding Security/Caching header mostly useful for static resources consumed by a browser. See
 * {@link HeaderConfiguration} for configuration properties and defaults.
 */
@WebFilter(filterName = AddHeadersFilter.FILTER_NAME, asyncSupported = true)
public class AddHeadersFilter extends OncePerRequestFilter {

    public static final String FILTER_NAME = "AddHeadersFilter";
    private static final Logger LOG = LoggerFactory.getLogger(AddHeadersFilter.class);

    private final ServletHeaders servletHeaders;
    private final HeaderConfiguration config;

    AddHeadersFilter(HeaderConfiguration config, ServletHeaders servletHeaders) {
        this.config = config;
        this.servletHeaders = servletHeaders;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,@NonNull FilterChain filterChain) throws ServletException, IOException {
        NoStoreOnErrorResponseWrapper statusAwareResponse = null;
        HttpServletResponse filteredResponse = response;
        if (shouldAddHeaders(request)) {
            statusAwareResponse = new NoStoreOnErrorResponseWrapper(response);
            filteredResponse = statusAwareResponse;
            try {
                addHeaders(request, statusAwareResponse);
            } catch (Exception ex) {
                LOG.warn("Failed to add security and caching headers to response", ex);
            }
        }

        boolean filterChainCompleted = false;
        try {
            filterChain.doFilter(request, filteredResponse);
            filterChainCompleted = true;
        } finally {
            updateCachingAfterFilterChain(statusAwareResponse, filterChainCompleted);
        }
    }

    private void updateCachingAfterFilterChain(NoStoreOnErrorResponseWrapper response, boolean filterChainCompleted) {
        if (response == null) {
            return;
        }
        try {
            if (filterChainCompleted) {
                response.preventCachingIfError();
            } else {
                response.preventCaching();
            }
        } catch (RuntimeException ex) {
            LOG.warn("Failed to update caching headers after filter chain processing", ex);
        }
    }

    private boolean shouldAddHeaders(HttpServletRequest request) {
        return config.getHttpMethods().contains(request.getMethod()) &&
                config.accept(getContextRelativeRequestPath(request));
    }

    private void addHeaders(HttpServletRequest request, HttpServletResponse response) {
        String path = getContextRelativeRequestPath(request);
        servletHeaders.addHeaders(response, request.getMethod(), path);
    }

    private static String getContextRelativeRequestPath(HttpServletRequest req) {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();
        StringBuilder url = new StringBuilder(servletPath);
        if (pathInfo != null) {
            url.append(pathInfo);
        }
        if (url.isEmpty()) {
            url.append("/");
        }
        return url.toString();
    }
}
