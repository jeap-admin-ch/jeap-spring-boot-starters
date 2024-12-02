package ch.admin.bit.jeap.starter.application.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Set;

/**
 * This class can be used as a bean annotated with @ControllerAdvice, or as a base class for custom controller advice
 * handlers. It will forward (possible) frontend route requests to /index.html, serving the contents of index.html
 * and return HTTP 200 OK. This is a desired behaviour for SPAs that serve frontend routes and backend APIs from
 * the same root context.
 */
@ControllerAdvice
public class FrontendRouteRedirectExceptionHandler extends ResponseEntityExceptionHandler {

    private final Set<String> nonFrontendRootPathParts;

    public FrontendRouteRedirectExceptionHandler() {
        nonFrontendRootPathParts = Set.of("api", "actuator");
    }

    public FrontendRouteRedirectExceptionHandler(Set<String> nonFrontendRootPathParts) {
        this.nonFrontendRootPathParts = Set.copyOf(nonFrontendRootPathParts);
    }

    /**
     * This handler makes sure that direct navigation to a route of a single page application is forwarded to index.html.
     * When then navigating inside the SPA in the browser, this handler is not invoked as the SPA code in the browser
     * takes care of route navigation.
     */
    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // If the request looks like a frontend route, return index.html
        if (mightBeFrontendRoute(request)) {
            return new ResponseEntity<>(new ClassPathResource("/static/index.html"), headers, 200);
        }
        // Otherwise, generate a 404 NOT FOUND response
        return super.handleNoResourceFoundException(ex, headers, status, request);
    }

    public boolean mightBeFrontendRoute(WebRequest webRequest) {
        return FrontendRouteMatcher.mightBeFrontendRouteWithIgnoreList(webRequest, nonFrontendRootPathParts);
    }

}
