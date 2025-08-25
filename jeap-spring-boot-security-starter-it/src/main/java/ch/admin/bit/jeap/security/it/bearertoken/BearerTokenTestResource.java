package ch.admin.bit.jeap.security.it.bearertoken;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class BearerTokenTestResource {

    public static final String API_PATH = "/api/bearertoken";
    
    private final WireMockServer wireMockServer;

    public BearerTokenTestResource() {
        this(0);
    }

    // user port = 0 for random port
    public BearerTokenTestResource(int port) {
        this.wireMockServer = new WireMockServer(wireMockConfig()
                        .port(port)
                        .extensions(new BearerTokenExtractorTransformer())
        );
    }

    public void start() {
        wireMockServer.start();
        stubDefaultBehavior();
    }

    public void stop() {
        wireMockServer.stop();
    }

    public void reset() {
        wireMockServer.resetAll();
        stubDefaultBehavior();
    }

    public String getBaseUrl() {
        return wireMockServer.baseUrl();
    }

    public String getBearerTokenUrl() {
        return getBaseUrl() + API_PATH;
    }

    private void stubDefaultBehavior() {
        wireMockServer.stubFor(get(urlEqualTo(API_PATH))
                        .willReturn(
                            aResponse()
                                .withTransformers(BearerTokenExtractorTransformer.NAME)));
    }

}
