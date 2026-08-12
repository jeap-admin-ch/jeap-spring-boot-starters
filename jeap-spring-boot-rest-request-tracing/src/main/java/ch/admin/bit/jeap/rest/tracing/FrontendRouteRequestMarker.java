package ch.admin.bit.jeap.rest.tracing;

import jakarta.servlet.ServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

/**
 * Marks a request as a frontend route of a single page application, i.e. a request that has not been handled by a
 * backend endpoint but has been answered with the SPA entry point (usually index.html).
 * <p>
 * Such requests are not backend endpoints and are therefore excluded from the security tracing that detects endpoints
 * being called without a JWT bearer token (metric {@code jeap_rest_endpoint_without_jwt}).
 * <p>
 * Applications serving frontend routes with their own handler instead of the
 * {@code FrontendRouteRedirectExceptionHandler} of the jeap application starter should mark such requests themselves.
 */
public final class FrontendRouteRequestMarker {

    /**
     * Name of the request attribute marking a request as a frontend route request.
     */
    public static final String FRONTEND_ROUTE_ATTRIBUTE = "ch.admin.bit.jeap.rest.tracing.frontendRoute";

    private FrontendRouteRequestMarker() {
    }

    /**
     * Marks the given request as a frontend route request.
     */
    public static void markAsFrontendRoute(ServletRequest request) {
        if (request != null) {
            request.setAttribute(FRONTEND_ROUTE_ATTRIBUTE, Boolean.TRUE);
        }
    }

    /**
     * Marks the given request as a frontend route request.
     */
    public static void markAsFrontendRoute(WebRequest webRequest) {
        if (webRequest != null) {
            webRequest.setAttribute(FRONTEND_ROUTE_ATTRIBUTE, Boolean.TRUE, RequestAttributes.SCOPE_REQUEST);
        }
    }

    /**
     * @return {@code true} if the given request has been marked as a frontend route request
     */
    public static boolean isFrontendRoute(ServletRequest request) {
        return (request != null) && Boolean.TRUE.equals(request.getAttribute(FRONTEND_ROUTE_ATTRIBUTE));
    }
}
