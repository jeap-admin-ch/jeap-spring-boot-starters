package ch.admin.bit.jeap.rest.tracing;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static ch.admin.bit.jeap.rest.tracing.FrontendRouteRequestMarker.FRONTEND_ROUTE_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;

class FrontendRouteRequestMarkerTest {

    @Test
    void isFrontendRoute_requestNotMarked_returnsFalse() {
        assertThat(FrontendRouteRequestMarker.isFrontendRoute(new MockHttpServletRequest())).isFalse();
    }

    @Test
    void isFrontendRoute_requestNull_returnsFalse() {
        assertThat(FrontendRouteRequestMarker.isFrontendRoute(null)).isFalse();
    }

    @Test
    void isFrontendRoute_attributeSetToOtherValue_returnsFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(FRONTEND_ROUTE_ATTRIBUTE, Boolean.FALSE);

        assertThat(FrontendRouteRequestMarker.isFrontendRoute(request)).isFalse();
    }

    @Test
    void markAsFrontendRoute_servletRequest_requestMarked() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        FrontendRouteRequestMarker.markAsFrontendRoute(request);

        assertThat(request.getAttribute(FRONTEND_ROUTE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(FrontendRouteRequestMarker.isFrontendRoute(request)).isTrue();
    }

    @Test
    void markAsFrontendRoute_webRequest_underlyingServletRequestMarked() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        FrontendRouteRequestMarker.markAsFrontendRoute(new ServletWebRequest(request));

        assertThat(request.getAttribute(FRONTEND_ROUTE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(FrontendRouteRequestMarker.isFrontendRoute(request)).isTrue();
    }

    @Test
    void markAsFrontendRoute_nullRequest_noException() {
        FrontendRouteRequestMarker.markAsFrontendRoute((HttpServletRequest) null);
        FrontendRouteRequestMarker.markAsFrontendRoute((WebRequest) null);
    }
}
