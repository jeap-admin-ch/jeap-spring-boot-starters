package ch.admin.bit.jeap.starter.application.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendRouteMatcherTest {

    @Test
    void mightBeFrontendRouteWithIgnoreList() {
        Set<String> nonFrontendRootPathParts = Set.of("api", "actuator");

        assertTrue(FrontendRouteMatcher.mightBeFrontendRouteWithIgnoreList(request("/route"), nonFrontendRootPathParts));
        assertTrue(FrontendRouteMatcher.mightBeFrontendRouteWithIgnoreList(request("/route/subroute"), nonFrontendRootPathParts));

        assertFalse(FrontendRouteMatcher.mightBeFrontendRouteWithIgnoreList(request("/test.css"), nonFrontendRootPathParts));
        assertFalse(FrontendRouteMatcher.mightBeFrontendRouteWithIgnoreList(request("/actuator/health"), nonFrontendRootPathParts));
        assertFalse(FrontendRouteMatcher.mightBeFrontendRouteWithIgnoreList(request("/actuator"), nonFrontendRootPathParts));
        assertFalse(FrontendRouteMatcher.mightBeFrontendRouteWithIgnoreList(request("/api/resource"), nonFrontendRootPathParts));
        assertFalse(FrontendRouteMatcher.mightBeFrontendRouteWithIgnoreList(request("/api"), nonFrontendRootPathParts));
        assertFalse(FrontendRouteMatcher.mightBeFrontendRouteWithIgnoreList(request("/ui-api/resource"), nonFrontendRootPathParts));
        assertFalse(FrontendRouteMatcher.mightBeFrontendRouteWithIgnoreList(request("/ui-api"), nonFrontendRootPathParts));
    }

    private static ServletWebRequest request(String path) {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest("GET", "http://host");
        mockHttpServletRequest.setServletPath(path);
        return new ServletWebRequest(mockHttpServletRequest);
    }
}
