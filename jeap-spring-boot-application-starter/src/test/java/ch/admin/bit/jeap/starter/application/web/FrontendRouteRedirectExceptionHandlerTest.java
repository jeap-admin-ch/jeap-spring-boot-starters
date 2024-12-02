package ch.admin.bit.jeap.starter.application.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendRouteRedirectExceptionHandlerTest {

    @Test
    void mightBeFrontendRoute() {
        FrontendRouteRedirectExceptionHandler handler = new FrontendRouteRedirectExceptionHandler();

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

    private static ServletWebRequest request(String path) {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest("GET", "http://host");
        mockHttpServletRequest.setServletPath(path);
        return new ServletWebRequest(mockHttpServletRequest);
    }
}
