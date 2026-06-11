package ch.admin.bit.jeap.web.configuration.servlet;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.equalTo;

/**
 * Reproduces and guards against the bug where the {@link org.springframework.web.filter.ShallowEtagHeaderFilter}
 * swallowed the body of {@code forward:}-ed responses (e.g. Spring Boot's welcome page forwarding {@code /} to
 * {@code index.html}). See {@link ServletWebConfiguration#disableEtagCachingOnForwardFilter()}.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.servlet.context-path=/test")
public class WelcomePageForwardEtagTest {

    @LocalServerPort
    private int port;

    private static String indexHtml() throws IOException {
        return new ClassPathResource("static/index.html").getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void welcomePageForward_servesFullBody() throws IOException {
        // GET /test/ triggers the welcome-page forward to index.html. Before the fix this body was empty.
        get("/test/").then()
                .statusCode(200)
                .body(equalTo(indexHtml()));
    }

    @Test
    void directRequest_servesFullBodyWithEtag() throws IOException {
        // Regression guard: non-forwarded requests keep their ETag behaviour intact.
        get("/test/index.html").then()
                .statusCode(200)
                .body(equalTo(indexHtml()))
                .header(HttpHeaders.ETAG, org.hamcrest.Matchers.notNullValue());
    }

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }
}
