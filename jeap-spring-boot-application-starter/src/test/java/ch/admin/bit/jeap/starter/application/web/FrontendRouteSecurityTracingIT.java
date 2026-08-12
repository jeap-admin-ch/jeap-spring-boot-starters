package ch.admin.bit.jeap.starter.application.web;

import ch.admin.bit.jeap.rest.tracing.security.RestResponseSecurityTrace;
import ch.admin.bit.jeap.rest.tracing.security.RestSecurityResponseListener;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Verifies that requests answered with the single page application entry point - either by the
 * {@link FrontendRouteRedirectExceptionHandler} or by the static resource handler - are not reported to the
 * {@link RestSecurityResponseListener}, i.e. that they do not show up in the metric jeap_rest_endpoint_without_jwt,
 * while requests reaching a backend endpoint still are.
 */
@SpringBootTest(classes = FrontendRouteSecurityTracingIT.TestApp.class, webEnvironment = RANDOM_PORT)
class FrontendRouteSecurityTracingIT {

    @LocalServerPort
    int localServerPort;

    @Autowired
    RecordingSecurityResponseListener securityResponseListener;

    @BeforeEach
    void clearTraces() {
        securityResponseListener.traces.clear();
    }

    @Test
    void frontendRoute_indexHtmlReturnedAndRequestNotTraced() {
        request().get("/product-groups")
                .then().assertThat()
                .statusCode(200)
                .body(containsString("single page application entry point"));

        assertThat(securityResponseListener.traces).isEmpty();
    }

    @Test
    void nestedFrontendRoute_indexHtmlReturnedAndRequestNotTraced() {
        request().get("/quota/product-groups/42")
                .then().assertThat()
                .statusCode(200)
                .body(containsString("single page application entry point"));

        assertThat(securityResponseListener.traces).isEmpty();
    }

    @Test
    void applicationRoot_indexHtmlReturnedAndRequestNotTraced() {
        request().get("/")
                .then().assertThat()
                .statusCode(200)
                .body(containsString("single page application entry point"));

        assertThat(securityResponseListener.traces).isEmpty();
    }

    @Test
    void staticResource_returnedAndRequestNotTraced() {
        request().get("/app.js")
                .then().assertThat()
                .statusCode(200)
                .body(containsString("spa asset"));

        assertThat(securityResponseListener.traces).isEmpty();
    }

    @Test
    void staticResourceIndexHtml_returnedAndRequestNotTraced() {
        request().get("/index.html")
                .then().assertThat()
                .statusCode(200);

        assertThat(securityResponseListener.traces).isEmpty();
    }

    @Test
    void restEndpoint_requestTraced() {
        request().get("/api/data")
                .then().assertThat()
                .statusCode(200);

        assertThat(securityResponseListener.traces)
                .extracting(RestResponseSecurityTrace::requestUriPattern)
                .containsExactly("/api/data");
    }

    @Test
    void endpointRespondingWithNotFound_requestTraced() {
        request().get("/api/missing-entity")
                .then().assertThat()
                .statusCode(404);

        assertThat(securityResponseListener.traces)
                .extracting(RestResponseSecurityTrace::requestUriPattern)
                .containsExactly("/api/missing-entity");
    }

    @Test
    void noFrontendRouteAndNoEndpoint_notFoundAndRequestNotTraced() {
        // No endpoint has been reached, the request ended up in the static resource handler
        request().get("/api/unknown")
                .then().assertThat()
                .statusCode(404);

        assertThat(securityResponseListener.traces).isEmpty();
    }

    @Test
    void staticResourceNotFound_notFoundAndRequestNotTraced() {
        request().get("/missing.css")
                .then().assertThat()
                .statusCode(404);

        assertThat(securityResponseListener.traces).isEmpty();
    }

    private RequestSpecification request() {
        return RestAssured.given().port(localServerPort);
    }

    static class RecordingSecurityResponseListener implements RestSecurityResponseListener {
        final List<RestResponseSecurityTrace> traces = new CopyOnWriteArrayList<>();

        @Override
        public void onResponse(RestResponseSecurityTrace restResponseTrace) {
            traces.add(restResponseTrace);
        }
    }

    @SpringBootApplication
    static class TestApp {

        @RestController
        static class TestController {
            @GetMapping("/api/data")
            String data() {
                return "data";
            }

            @GetMapping("/api/missing-entity")
            ResponseEntity<String> missingEntity() {
                return ResponseEntity.notFound().build();
            }
        }

        @Bean
        FrontendRouteRedirectExceptionHandler frontendRouteRedirectExceptionHandler() {
            return new FrontendRouteRedirectExceptionHandler();
        }

        @Bean
        RecordingSecurityResponseListener recordingSecurityResponseListener() {
            return new RecordingSecurityResponseListener();
        }
    }
}
