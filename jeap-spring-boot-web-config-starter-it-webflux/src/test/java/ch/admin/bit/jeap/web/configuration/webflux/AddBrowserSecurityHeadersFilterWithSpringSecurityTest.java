package ch.admin.bit.jeap.web.configuration.webflux;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.get;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jeap.security.oauth2.resourceserver.authorization-server.issuer=http://issuer",
                "spring.application.name=test"})
public class AddBrowserSecurityHeadersFilterWithSpringSecurityTest {

    private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    private static final String CSP_VALUE = "default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; font-src 'self'; img-src 'self'; " +
            "connect-src 'self' http://issuer; " +
            "frame-src 'self' http://issuer; " +
            "frame-ancestors 'self'";
    private static final String CACHE_CONTROL_VALUE = "no-cache, no-store, max-age=0, must-revalidate";

    @LocalServerPort
    private int port;

    @Test
    void expect_filter_headers_to_override_spring_security_headers() {
        // Added by filter
        get("/index.html").then()
                .statusCode(200)
                .header(CONTENT_SECURITY_POLICY, CSP_VALUE);
        // Added by spring security
        get("/api/resource").then()
                .statusCode(200)
                .header(CACHE_CONTROL, CACHE_CONTROL_VALUE);
        get("/ui-api/resource").then()
                .statusCode(200)
                .header(CACHE_CONTROL, CACHE_CONTROL_VALUE);
        get("/some-consumer-api/resource").then()
                .statusCode(200)
                .header(CACHE_CONTROL, CACHE_CONTROL_VALUE);

        // No browser security headers for api responses
        boolean hasCspHeader = get("/api").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(CONTENT_SECURITY_POLICY);
        assertFalse(hasCspHeader);
        boolean hasUIAPICspHeader = get("/ui-api").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(CONTENT_SECURITY_POLICY);
        assertFalse(hasUIAPICspHeader);
        boolean hasConsumerApiAPICspHeader = get("/some-consumer-api").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(CONTENT_SECURITY_POLICY);
        assertFalse(hasConsumerApiAPICspHeader);
    }

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }
}
