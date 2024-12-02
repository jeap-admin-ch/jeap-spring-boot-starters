package ch.admin.bit.jeap.swagger.webflux.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

//Disabled is default, so no additional parameters need to be set
@SpringBootTest(classes = TestConfiguration.class)
@AutoConfigureWebTestClient
class DisabledIT {
    private final static String SWAGGER_REDIRECT_URL = "/swagger-ui.html";
    private final static String SWAGGER_UI_URL = "/webjars/swagger-ui/index.html";
    private final static String SWAGGER_CONFIG_URL = "/api-docs/swagger-config";
    private final static String OPEN_API_SPEC_URL = "/api-docs/test";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void swaggerRedirectNotReachable() {
        webTestClient.get()
                .uri(SWAGGER_REDIRECT_URL)
                .exchange()
                .expectStatus().isEqualTo(401);
    }

    @Test
    void swaggerUINotReachable() {
        webTestClient.get()
                .uri(SWAGGER_UI_URL)
                .exchange()
                .expectStatus().isEqualTo(401);
    }

    @Test
    void swaggerConfigNotReachable() {
        webTestClient.get()
                .uri(SWAGGER_CONFIG_URL)
                .exchange()
                .expectStatus().isEqualTo(401);
    }

    @Test
    void openApiNotReachable() {
        webTestClient.get()
                .uri(OPEN_API_SPEC_URL)
                .exchange()
                .expectStatus().isEqualTo(401);
    }
}
