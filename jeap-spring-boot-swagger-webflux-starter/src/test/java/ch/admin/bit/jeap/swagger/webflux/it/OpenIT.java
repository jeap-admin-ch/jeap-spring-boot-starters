package ch.admin.bit.jeap.swagger.webflux.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(classes = TestConfiguration.class, properties = {"jeap.swagger.status=OPEN", "springdoc.swagger-ui.oauth2-redirect-url=http://localhost/webjars/swagger-ui/oauth2-redirect.html"})
@AutoConfigureWebTestClient
class OpenIT {
    private final static String SWAGGER_REDIRECT_URL = "/swagger-ui.html";
    private final static String SWAGGER_UI_URL = "/webjars/swagger-ui/index.html";
    private final static String SWAGGER_CONFIG_URL = "/api-docs/swagger-config";
    private final static String OPEN_API_SPEC_URL = "/api-docs/test";
    private final static String OAUTH_REDIRECT_URL = "http://localhost/webjars/swagger-ui/oauth2-redirect.html";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void swaggerRedirect() {
        webTestClient.get()
                .uri(SWAGGER_REDIRECT_URL)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", SWAGGER_UI_URL);
    }

    @Test
    void swaggerUIReachable() {
        webTestClient.get()
                .uri(SWAGGER_UI_URL)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML);
    }

    @Test
    void swaggerConfig() {
        webTestClient.get()
                .uri(SWAGGER_CONFIG_URL)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .json("{'configUrl':'" + SWAGGER_CONFIG_URL + "'}")
                .json("{'oauth2RedirectUrl':'" + OAUTH_REDIRECT_URL + "'}")
                .json("{'urls':[{'url':'" + OPEN_API_SPEC_URL + "','name':'test'}]}");
    }

    @Test
    void openApi() {
        webTestClient.get()
                .uri(OPEN_API_SPEC_URL)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("{'openapi':'3.0.1'}");
    }
}
