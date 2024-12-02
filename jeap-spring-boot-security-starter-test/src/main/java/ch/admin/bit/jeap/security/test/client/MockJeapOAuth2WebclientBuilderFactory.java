package ch.admin.bit.jeap.security.test.client;

import ch.admin.bit.jeap.security.client.JeapOAuth2WebclientBuilderFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Mock implementation of the JeapOAuth2WebclientBuilderFactory for integration test purposes.
 * WebClient.Builder instances created with this mock build WebClient instances that fetch their OAuth2 token from a
 * AuthTokenProvider class instance instead of an external authorization server. The authorization token stored in the
 * AuthTokenProvider can be set as needed in an integration test. Such integration tests should not be run in parallel
 * on the same JeapOAuth2WebclientBuilderFactory as one AuthTokenProvider instance is shared by all WebClients built with
 * the mock factory.
 */
@Slf4j
public class MockJeapOAuth2WebclientBuilderFactory  implements JeapOAuth2WebclientBuilderFactory {

    @Getter
    private final AuthTokenProvider authTokenProvider = new AuthTokenProvider();

    private final WebClient.Builder webClientBuilder;

    public MockJeapOAuth2WebclientBuilderFactory(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder.clone();
    }

    @Override
    public WebClient.Builder createForClientId(String clientRegistryId) {
        return createForAuthTokenProvider();
    }

    @Override
    public WebClient.Builder createForClientRegistryId(String clientRegistryId) {
        return createForAuthTokenProvider();
    }

    @Override
    public WebClient.Builder createForClientIdPreferringTokenFromIncomingRequest(String clientRegistryId) {
        return createForAuthTokenProvider();
    }

    @Override
    public WebClient.Builder createForClientRegistryIdPreferringTokenFromIncomingRequest(String clientRegistryId) {
        return createForAuthTokenProvider();
    }

    @Override
    public WebClient.Builder createForTokenFromIncomingRequest() {
        return createForAuthTokenProvider();
    }

    private WebClient.Builder createForAuthTokenProvider() {
        return webClientBuilder.clone().
                filter( (request, next) ->
                        next.exchange(ClientRequest.from(request).
                                headers(headers -> {
                                    String authToken = authTokenProvider.getAuthToken();
                                    headers.setBearerAuth(authToken);
                                    log.debug("Setting the Authorization header with the authentication token '{}'.", authToken);
                                }).
                                build()
                        )
                );
    }

}
