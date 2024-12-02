package ch.admin.bit.jeap.web.configuration.servlet;

import ch.admin.bit.jeap.monitor.ActuatorSecurity;
import ch.admin.bit.jeap.security.test.configuration.DisableJeapSecurityStarterAutoConfiguration;
import ch.admin.bit.jeap.security.test.resource.configuration.DisableJeapPermitAllSecurityConfiguration;
import ch.admin.bit.jeap.web.configuration.AbstractHeaders;
import ch.admin.bit.jeap.web.configuration.HttpHeaderFilterPostProcessor;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, ActuatorSecurity.class, ManagementWebSecurityAutoConfiguration.class})
@Import({DisableJeapSecurityStarterAutoConfiguration.class, DisableJeapPermitAllSecurityConfiguration.class})
public class AddBrowserSecurityHeadersFilterWithoutSpringSecurityTest {

    private static final String STRICT_ORIGIN_WHEN_CROSS_ORIGIN = "strict-origin-when-cross-origin";
    private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    private static final String X_XSS_PROTECTION = "X-XSS-Protection";

    @LocalServerPort
    private int port;

    @TestConfiguration
    static class TestConfig {
        @Bean
        HttpHeaderFilterPostProcessor httpHeaderFilterPostProcessor() {
            return new HttpHeaderFilterPostProcessor() {
                @Override
                public void postProcessHeaders(Map<String, String> headers, String method, String path) {
                    headers.put("Custom-Header", "Test");
                }
            };
        }
    }

    @Test
    void expect_api_resources_to_not_have_headers_configured() {
        boolean hasRootCspHeader = get("/api").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(CONTENT_SECURITY_POLICY);
        boolean hasResourceCspHeader = get("/api/resource").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(CONTENT_SECURITY_POLICY);
        boolean hasXssHeader = get("/api/resource").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(X_XSS_PROTECTION);
        boolean hasCacheControlHeader = get("/api/resource").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(CACHE_CONTROL);

        assertFalse(hasRootCspHeader);
        assertFalse(hasResourceCspHeader);
        assertFalse(hasXssHeader);
        assertFalse(hasCacheControlHeader);
    }

    @Test
    void expect_ui_api_resources_to_not_have_headers_configured() {
        boolean hasRootCspHeader = get("/ui-api").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(CONTENT_SECURITY_POLICY);
        boolean hasResourceCspHeader = get("/ui-api/resource").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(CONTENT_SECURITY_POLICY);
        boolean hasXssHeader = get("/ui-api/resource").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(X_XSS_PROTECTION);
        boolean hasCacheControlHeader = get("/ui-api/resource").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(CACHE_CONTROL);

        assertFalse(hasRootCspHeader);
        assertFalse(hasResourceCspHeader);
        assertFalse(hasXssHeader);
        assertFalse(hasCacheControlHeader);
    }

    @Test
    void expect_custom_consumer_api_resources_to_not_have_headers_configured() {
        boolean hasRootCspHeader = get("/some-consumer-api").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(CONTENT_SECURITY_POLICY);
        boolean hasResourceCspHeader = get("/some-consumer-api/resource").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(CONTENT_SECURITY_POLICY);
        boolean hasXssHeader = get("/some-consumer-api/resource").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(X_XSS_PROTECTION);
        boolean hasCacheControlHeader = get("/some-consumer-api/resource").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(CACHE_CONTROL);

        assertFalse(hasRootCspHeader);
        assertFalse(hasResourceCspHeader);
        assertFalse(hasXssHeader);
        assertFalse(hasCacheControlHeader);
    }

    @Test
    void expect_only_configured_methods_to_have_headers_configured() {
        boolean getHasHeader = get("/notapi/resource").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(CONTENT_SECURITY_POLICY);
        boolean headHasHeader = head("/notapi/resource").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(CONTENT_SECURITY_POLICY);
        boolean postHasHeader = post("/notapi/resource")
                .then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(CONTENT_SECURITY_POLICY);

        assertTrue(getHasHeader);
        assertTrue(headHasHeader);
        assertFalse(postHasHeader);
    }

    @Test
    void expect_static_resources_to_have_headers_configured() {
        get("/index.html").then()
                .statusCode(200)
                .header(AbstractHeaders.REFERRER_POLICY, STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .extract().headers().hasHeaderWithName(CACHE_CONTROL);
        get("/test.js").then()
                .statusCode(200)
                .header(AbstractHeaders.REFERRER_POLICY, STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .extract().headers().hasHeaderWithName(CACHE_CONTROL);
        get("/asset/api").then()
                .statusCode(200)
                .header(AbstractHeaders.REFERRER_POLICY, STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .extract().headers().hasHeaderWithName(CACHE_CONTROL);
    }

    @Test
    void expect_custom_header_to_be_added() {
        get("/").then()
                .statusCode(200)
                .header("Custom-Header", "Test")
                .extract().headers().hasHeaderWithName(CACHE_CONTROL);
    }

    @Test
    void expect_root_resource_to_have_headers_configured() {
        get("/").then()
                .statusCode(200)
                .header(AbstractHeaders.REFERRER_POLICY, STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .extract().headers().hasHeaderWithName(CACHE_CONTROL);
        get("").then()
                .statusCode(200)
                .header(AbstractHeaders.REFERRER_POLICY, STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .extract().headers().hasHeaderWithName(CACHE_CONTROL);
    }

    @Test
    void expect_noptapi_and_async_resources_to_have_headers_configured() {
        get("/notapi/async-resource").then()
                .statusCode(200)
                .header(AbstractHeaders.REFERRER_POLICY, STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .extract().headers().hasHeaderWithName(CACHE_CONTROL);
        get("/notapi/resource").then()
                .statusCode(200)
                .header(AbstractHeaders.REFERRER_POLICY, STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .extract().headers().hasHeaderWithName(CACHE_CONTROL);
    }

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }
}
