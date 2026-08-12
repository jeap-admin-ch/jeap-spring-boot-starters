package ch.admin.bit.jeap.starter.application.web;

import ch.admin.bit.jeap.rest.tracing.FrontendRouteRequestMarker;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendRouteRedirectExceptionHandlerTest {

    private final FrontendRouteRedirectExceptionHandler handler = new FrontendRouteRedirectExceptionHandler();

    @Test
    void mightBeFrontendRoute() {
        assertTrue(handler.mightBeFrontendRoute(request("/route")));
        assertTrue(handler.mightBeFrontendRoute(request("/route/subroute")));

        assertFalse(handler.mightBeFrontendRoute(request("/test.css")));
        assertFalse(handler.mightBeFrontendRoute(request("/actuator/health")));
        assertFalse(handler.mightBeFrontendRoute(request("/actuator")));
        assertFalse(handler.mightBeFrontendRoute(request("/api/resource")));
        assertFalse(handler.mightBeFrontendRoute(request("/api")));
        assertFalse(handler.mightBeFrontendRoute(request("/ui-api/resource")));
        assertFalse(handler.mightBeFrontendRoute(request("/ui-api")));
    }

    @Test
    void handleNoResourceFoundException_frontendRoute_indexHtmlReturnedAndRequestMarkedAsFrontendRoute() {
        ServletWebRequest webRequest = request("/route/subroute");

        ResponseEntity<Object> response = handleNoResourceFound(webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(ClassPathResource.class);
        assertThat(FrontendRouteRequestMarker.isFrontendRoute(webRequest.getRequest())).isTrue();
    }

    @Test
    void handleNoResourceFoundException_noFrontendRoute_notFoundReturnedAndRequestNotMarkedAsFrontendRoute() {
        ServletWebRequest webRequest = request("/api/unknown");

        ResponseEntity<Object> response = handleNoResourceFound(webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(FrontendRouteRequestMarker.isFrontendRoute(webRequest.getRequest())).isFalse();
    }

    @Test
    void handleNoResourceFoundException_customNonFrontendRootPathPart_requestNotMarkedAsFrontendRoute() {
        FrontendRouteRedirectExceptionHandler customHandler =
                new FrontendRouteRedirectExceptionHandler(Set.of("backend"));
        ServletWebRequest webRequest = request("/backend/resource");

        ResponseEntity<Object> response = customHandler.handleNoResourceFoundException(
                noResourceFoundException(webRequest), new HttpHeaders(), HttpStatus.NOT_FOUND, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(FrontendRouteRequestMarker.isFrontendRoute(webRequest.getRequest())).isFalse();
    }

    private ResponseEntity<Object> handleNoResourceFound(ServletWebRequest webRequest) {
        return handler.handleNoResourceFoundException(
                noResourceFoundException(webRequest), new HttpHeaders(), HttpStatus.NOT_FOUND, webRequest);
    }

    private static NoResourceFoundException noResourceFoundException(ServletWebRequest webRequest) {
        String path = webRequest.getRequest().getServletPath();
        return new NoResourceFoundException(HttpMethod.GET, path, path);
    }

    private static ServletWebRequest request(String path) {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest("GET", "http://host");
        mockHttpServletRequest.setServletPath(path);
        return new ServletWebRequest(mockHttpServletRequest, new MockHttpServletResponse());
    }
}
