package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.restclient.JeapOAuth2RestClientBuilderFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

import static ch.admin.bit.jeap.security.it.resource.OAuth2TestGateway.RestClientTokenSource.*;

@RestController
public class OAuth2TestGateway {

    public static final String API_PATH = "/api/gateway";
    public static final String TOKEN_SOURCE_PARAM_NAME = "rest-client-token-source";

    public enum RestClientTokenSource {
        CLIENT,
        REQUEST,
        REQUEST_ELSE_CLIENT
    }

    private static final String REST_CLIENT_ID = "test-client";

    private final Map<RestClientTokenSource, RestClient> restClients = new HashMap<>();

    public OAuth2TestGateway(JeapOAuth2RestClientBuilderFactory jeapOAuth2RestClientBuilderFactory, String targetResourceUrl) {
        restClients.put(CLIENT, jeapOAuth2RestClientBuilderFactory.createForClientRegistryId(REST_CLIENT_ID).baseUrl(targetResourceUrl).build());
        restClients.put(REQUEST, jeapOAuth2RestClientBuilderFactory.createForTokenFromIncomingRequest().baseUrl(targetResourceUrl).build());
        restClients.put(REQUEST_ELSE_CLIENT, jeapOAuth2RestClientBuilderFactory.createForClientRegistryIdPreferringTokenFromIncomingRequest(REST_CLIENT_ID).baseUrl(targetResourceUrl).build());
    }

    @GetMapping(API_PATH)
    public String forward(@RequestParam(TOKEN_SOURCE_PARAM_NAME) RestClientTokenSource restClientTokenSource) {
        RestClient callingRestClient = restClients.get(restClientTokenSource);
        if (callingRestClient == null) {
            throw new IllegalArgumentException("Unsupported RestClientTokenSource type.");
        }
        return callingRestClient.get().retrieve().body(String.class);
    }
}
