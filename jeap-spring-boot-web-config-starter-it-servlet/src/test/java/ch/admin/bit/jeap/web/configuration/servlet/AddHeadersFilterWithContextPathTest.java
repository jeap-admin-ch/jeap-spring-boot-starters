package ch.admin.bit.jeap.web.configuration.servlet;

import ch.admin.bit.jeap.web.configuration.AbstractHeaders;
import io.restassured.RestAssured;
import io.restassured.http.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;

import static io.restassured.RestAssured.get;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.servlet.context-path=/test")
public class AddHeadersFilterWithContextPathTest {

    private static final String STRICT_ORIGIN_WHEN_CROSS_ORIGIN = "strict-origin-when-cross-origin";

    @LocalServerPort
    private int port;

    @Test
    void expect_api_resources_to_not_have_security_headers_configured() {
        boolean hasRootReferrerPolicyHeader = get("/test/api").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(AbstractHeaders.REFERRER_POLICY);
        boolean hasResourceReferrerPolicyHeader = get("/test/api/resource").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(AbstractHeaders.REFERRER_POLICY);

        assertFalse(hasRootReferrerPolicyHeader);
        assertFalse(hasResourceReferrerPolicyHeader);
    }

    @Test
    void expect_ui_api_resources_to_not_have_security_headers_configured() {
        boolean hasRootReferrerPolicyHeader = get("/test/ui-api").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(AbstractHeaders.REFERRER_POLICY);
        boolean hasResourceReferrerPolicyHeader = get("/test/ui-api/resource").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(AbstractHeaders.REFERRER_POLICY);

        assertFalse(hasRootReferrerPolicyHeader);
        assertFalse(hasResourceReferrerPolicyHeader);
    }

    @Test
    void expect_custom_consumer_api_resources_to_not_have_security_headers_configured() {
        boolean hasRootReferrerPolicyHeader = get("/test/some-consumer-api").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(AbstractHeaders.REFERRER_POLICY);
        boolean hasResourceReferrerPolicyHeader = get("/test/some-consumer-api/resource").then()
                .statusCode(200)
                .extract().headers().hasHeaderWithName(AbstractHeaders.REFERRER_POLICY);

        assertFalse(hasRootReferrerPolicyHeader);
        assertFalse(hasResourceReferrerPolicyHeader);
    }

    @Test
    void expect_actuator_resources_to_not_have_security_headers_configured() {
        Headers headers = get("/test/actuator/info").then()
                .statusCode(200)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate")
                .extract().headers();
        boolean hasReferrerPolicyHeader =
                headers.hasHeaderWithName(AbstractHeaders.REFERRER_POLICY);

        assertFalse(hasReferrerPolicyHeader);
    }

    @Test
    void expect_static_resources_to_have_security_headers_configured() {
        get("/test/index.html").then()
                .statusCode(200)
                .header(AbstractHeaders.REFERRER_POLICY, STRICT_ORIGIN_WHEN_CROSS_ORIGIN);
        get("/test/test.js").then()
                .statusCode(200)
                .header(AbstractHeaders.REFERRER_POLICY, STRICT_ORIGIN_WHEN_CROSS_ORIGIN);
    }

    @Test
    void expect_root_resource_to_have_security_headers_configured() {
        get("/test").then()
                .statusCode(200)
                .header(AbstractHeaders.REFERRER_POLICY, STRICT_ORIGIN_WHEN_CROSS_ORIGIN);
        get("/test/").then()
                .statusCode(200)
                .header(AbstractHeaders.REFERRER_POLICY, STRICT_ORIGIN_WHEN_CROSS_ORIGIN);
    }

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }
}
