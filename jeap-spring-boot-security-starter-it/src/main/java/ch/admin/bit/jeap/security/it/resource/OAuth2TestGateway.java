package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.client.JeapOAuth2WebclientBuilderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static ch.admin.bit.jeap.security.it.resource.OAuth2TestGateway.WebClientTokenSource.*;

@RestController
public class OAuth2TestGateway {

    public static final String API_PATH = "/api/gateway";
    public static final String TOKEN_SOURCE_PARAM_NAME = "web-client-token-source";
    public enum WebClientTokenSource {
        CLIENT,
        REQUEST,
        REQUEST_ELSE_CLIENT
    }

    private static final String WEBCLIENT_ID = "test-client";

    private final Map<WebClientTokenSource, WebClient> webClients = new HashMap<>();

    public OAuth2TestGateway(JeapOAuth2WebclientBuilderFactory jeapOAuth2WebclientBuilderFactory, @Value("${server.port}") int serverPort) {
        String resourceBaseUrl = "http://localhost:" + serverPort + BearerTokenResource.API_PATH;
        webClients.put(CLIENT, jeapOAuth2WebclientBuilderFactory.createForClientId(WEBCLIENT_ID).baseUrl(resourceBaseUrl).build());
        webClients.put(REQUEST, jeapOAuth2WebclientBuilderFactory.createForTokenFromIncomingRequest().baseUrl(resourceBaseUrl).build());
        webClients.put(REQUEST_ELSE_CLIENT, jeapOAuth2WebclientBuilderFactory.createForClientIdPreferringTokenFromIncomingRequest(WEBCLIENT_ID).baseUrl(resourceBaseUrl).build());
    }

    @GetMapping(API_PATH)
    public Mono<String> forward(@RequestParam(TOKEN_SOURCE_PARAM_NAME) WebClientTokenSource webClientTokenSource) {
        WebClient callingWebClient = webClients.get(webClientTokenSource);
        if (callingWebClient == null) {
            throw new IllegalArgumentException("Unsupported WebClientTokenSource type.");
        }
        return callingWebClient.get().retrieve().bodyToMono(String.class);
    }

}
