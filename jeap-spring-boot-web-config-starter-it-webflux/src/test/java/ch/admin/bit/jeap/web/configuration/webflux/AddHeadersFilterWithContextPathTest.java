package ch.admin.bit.jeap.web.configuration.webflux;

import ch.admin.bit.jeap.web.configuration.AbstractHeaders;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.webflux.base-path=/test",
                "jeap.security.oauth2.resourceserver.authorization-server.issuer=http://issuer",
                "spring.application.name=test"})
class AddHeadersFilterWithContextPathTest {

    private static final String STRICT_ORIGIN_WHEN_CROSS_ORIGIN = "strict-origin-when-cross-origin";

    @LocalServerPort
    private int port;

    @Test
    void expect_api_resources_to_not_have_security_headers_configured() {
        String rootReferrerPolicyHeader = get("/test/api").then()
                .statusCode(200)
                .extract().headers().getValue(AbstractHeaders.REFERRER_POLICY);
        String resourceReferrerPolicyHeader = get("/test/api/resource").then()
                .statusCode(200)
                .extract().headers().getValue(AbstractHeaders.REFERRER_POLICY);

        assertThat(rootReferrerPolicyHeader).isEqualTo("no-referrer");
        assertThat(resourceReferrerPolicyHeader).isEqualTo("no-referrer");
    }

    @Test
    void expect_ui_api_resources_to_not_have_security_headers_configured() {
        String rootReferrerPolicyHeader = get("/test/ui-api").then()
                .statusCode(200)
                .extract().headers().getValue(AbstractHeaders.REFERRER_POLICY);
        String resourceReferrerPolicyHeader = get("/test/ui-api/resource").then()
                .statusCode(200)
                .extract().headers().getValue(AbstractHeaders.REFERRER_POLICY);

        assertThat(rootReferrerPolicyHeader).isEqualTo("no-referrer");
        assertThat(resourceReferrerPolicyHeader).isEqualTo("no-referrer");
    }

    @Test
    void expect_custom_consumer_api_resources_to_not_have_security_headers_configured() {
        String rootReferrerPolicyHeader = get("/test/some-consumer-api").then()
                .statusCode(200)
                .extract().headers().getValue(AbstractHeaders.REFERRER_POLICY);
        String resourceReferrerPolicyHeader = get("/test/some-consumer-api/resource").then()
                .statusCode(200)
                .extract().headers().getValue(AbstractHeaders.REFERRER_POLICY);

        assertThat(rootReferrerPolicyHeader).isEqualTo("no-referrer");
        assertThat(resourceReferrerPolicyHeader).isEqualTo("no-referrer");
    }

    @Test
    void expect_static_resources_to_have_security_headers_configured() {
        String indexHeader = get("/test/index.html").then()
                .statusCode(200)
                .extract().headers().getValue(AbstractHeaders.REFERRER_POLICY);
        String testHeader = get("/test/test.js").then()
                .statusCode(200)
                .extract().headers().getValue(AbstractHeaders.REFERRER_POLICY);

        assertThat(indexHeader).isEqualTo(STRICT_ORIGIN_WHEN_CROSS_ORIGIN);
        assertThat(testHeader).isEqualTo(STRICT_ORIGIN_WHEN_CROSS_ORIGIN);
    }

    @Test
    void expect_root_resource_to_have_security_headers_configured() {
        String testHeader = get("/test/").then()
                .statusCode(200)
                .extract().headers().getValue(AbstractHeaders.REFERRER_POLICY);
        assertThat(testHeader).isEqualTo(STRICT_ORIGIN_WHEN_CROSS_ORIGIN);
    }

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }
}
