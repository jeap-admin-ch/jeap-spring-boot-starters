package ch.admin.bit.jeap.security.test.client;

import ch.admin.bit.jeap.security.restclient.JeapOAuth2RestClientBuilderFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

/**
 * Mock implementation of the JeapOAuth2RestClientBuilderFactory for integration test purposes.
 * RestClient.Builder instances created with this mock build RestClient instances that fetch their OAuth2 token from a
 * AuthTokenProvider class instance instead of an external authorization server. The authorization token stored in the
 * AuthTokenProvider can be set as needed in an integration test. Such integration tests should not be run in parallel
 * on the same JeapOAuth2RestClientBuilderFactory as one AuthTokenProvider instance is shared by all RestClients built with
 * the mock factory.
 */
@Slf4j
public class MockJeapOAuth2RestClientBuilderFactory implements JeapOAuth2RestClientBuilderFactory {

    @Getter
    private final AuthTokenProvider authTokenProvider = new AuthTokenProvider();

    private final RestClient.Builder restClientBuilder;

    public MockJeapOAuth2RestClientBuilderFactory(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder.clone();
    }

    @Override
    public RestClient.Builder createForClientRegistryId(String clientRegistryId) {
        return createForAuthTokenProvider();
    }

    @Override
    public RestClient.Builder createForClientRegistryIdPreferringTokenFromIncomingRequest(String clientRegistryId) {
        return createForAuthTokenProvider();
    }

    @Override
    public RestClient.Builder createForTokenFromIncomingRequest() {
        return createForAuthTokenProvider();
    }

    private RestClient.Builder createForAuthTokenProvider() {
        return restClientBuilder.clone()
                .requestInitializer(new OAuth2ClientAuthTokenProviderRestClientInitializer(authTokenProvider));
    }

}
