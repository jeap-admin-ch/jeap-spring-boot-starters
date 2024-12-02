package ch.admin.bit.jeap.web.configuration.servlet;

import ch.admin.bit.jeap.web.configuration.HeaderConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            addHeaders(request, response);
        } catch (Exception ex) {
            LOG.warn("Failed to add security and caching headers to response", ex);
        }
        filterChain.doFilter(request, response);
    }

    private void addHeaders(HttpServletRequest request, HttpServletResponse response) {
        if (config.getHttpMethods().contains(request.getMethod())) {
            String path = getContextRelativeRequestPath(request);
            addHeaders(response, request.getMethod(), path);
        }
    }

    private void addHeaders(HttpServletResponse response, String method, String path) {
        if (config.accept(path)) {
            servletHeaders.addHeaders(response, method, path);
        }
    }

    private static String getContextRelativeRequestPath(HttpServletRequest req) {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();
        StringBuilder url = new StringBuilder(servletPath);
        if (pathInfo != null) {
            url.append(pathInfo);
        }
        if (url.length() == 0) {
            url.append("/");
        }
        return url.toString();
    }
}
