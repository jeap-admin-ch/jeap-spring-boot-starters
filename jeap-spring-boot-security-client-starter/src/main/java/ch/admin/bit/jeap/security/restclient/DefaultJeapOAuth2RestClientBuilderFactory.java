package ch.admin.bit.jeap.security.restclient;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

public class DefaultJeapOAuth2RestClientBuilderFactory implements JeapOAuth2RestClientBuilderFactory {

    private final RestClient.Builder restClientBuilder;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuth2AuthenticationTokenRestClientInitializer oAuth2AuthenticationTokenRestClientInitializer = new OAuth2AuthenticationTokenRestClientInitializer();

    public DefaultJeapOAuth2RestClientBuilderFactory(RestClient.Builder restClientBuilder, OAuth2AuthorizedClientManager authorizedClientManager, ClientRegistrationRepository clientRegistrationRepository) {
        this.restClientBuilder = restClientBuilder.clone();
        this.authorizedClientManager = authorizedClientManager;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    public RestClient.Builder createForClientRegistryId(String clientRegistryId) {
        assertOAuth2ClientConfigured(clientRegistryId);
        return restClientBuilder.clone()
                .requestInitializer(new OAuth2ClientCredentialsRestClientInitializer(authorizedClientManager, clientRegistrationRepository.findByRegistrationId(clientRegistryId), false));
    }

    @Override
    public RestClient.Builder createForClientRegistryIdPreferringTokenFromIncomingRequest(String clientRegistryId) {
        assertOAuth2ClientConfigured(clientRegistryId);
        return restClientBuilder.clone()
                .requestInitializer(oAuth2AuthenticationTokenRestClientInitializer)
                .requestInitializer(new OAuth2ClientCredentialsRestClientInitializer(authorizedClientManager, clientRegistrationRepository.findByRegistrationId(clientRegistryId), true));
    }

    @Override
    public RestClient.Builder createForTokenFromIncomingRequest() {
        return restClientBuilder.clone()
                .requestInitializer(oAuth2AuthenticationTokenRestClientInitializer);
    }

    private void assertOAuth2ClientConfigured(String clientRegistryId) {
        if (authorizedClientManager == null || clientRegistrationRepository == null) {
            throw new UnsupportedOperationException("Application not configured as OAuth2 client.");
        }
        if (clientRegistrationRepository.findByRegistrationId(clientRegistryId) == null) {
            throw new IllegalArgumentException("There is no client registration with id '" + clientRegistryId + "' configured.");
        }
    }

}
