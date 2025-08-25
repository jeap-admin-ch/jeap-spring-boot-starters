package ch.admin.bit.jeap.security.restclient;

import org.springframework.web.client.RestClient;

/**
 * <pre>
 * Interface specification for a RestClient builder factory that can create RestClient.Builder instances that build
 * RestClient instances that automatically add an OAuth2 access token as a bearer token to RestClient exchanges.
 *
 * There are different factory methods for creating RestClients with different possible sources for the access tokens.
 *
 * One possibility is to configure the RestClient as an OAuth2 client for a given client registration and fetch access tokens
 * from an authorization server by means of the OAuth2 client credentials flow. The OAuth2 client configuration must be
 * provided using the standard spring boot security configuration properties (spring.security.oauth2.client.registration.*).
 *
 * Another possibility is to configure the RestClient to carry over the access token from the incoming request it is executed in.
 *
 * A third possibility is to combine the first two possibilities.
 * </pre>
 */
public interface JeapOAuth2RestClientBuilderFactory {

    /**
     * Creates a RestClient.Builder instance that is configured to build RestClient instances that augment exchanges
     * with OAuth2 access tokens created from the client configuration identified.
     *
     * @param clientRegistryId Identifier to select one of the configured OAuth2 client registry configurations.
     * @return A RestClient.Builder instance preconfigured to build RestClient instances that act as an OAuth2 client.
     */
    RestClient.Builder createForClientRegistryId(String clientRegistryId);

    /**
     * Creates a RestClient.Builder instance that is configured to build RestClient instances that augment exchanges
     * with OAuth2 access tokens. A created RestClient instance carries over the OAuth2 token from the incoming request it is
     * executed in if such a token is present, otherwise the RestClient instance uses an access token created from the
     * client configuration identified.
     *
     * @param clientRegistryId Identifier to select one of the configured OAuth2 client registry configurations.
     * @return A RestClient.Builder instance preconfigured to build RestClient instances that augment exchanges with OAuth2 access tokens.
     */
    RestClient.Builder createForClientRegistryIdPreferringTokenFromIncomingRequest(String clientRegistryId);

    /**
     * Creates a RestClient.Builder instance that is configured to build RestClient instances that augment every exchange
     * with the OAuth2 access token carried over from the current incoming request, i.e. a created RestClient instance 'reuses'
     * the OAuth2 access token of the incoming request it is executed in.
     *
     * @return A RestClient.Builder instance preconfigured to build RestClient instances that augment exchanges with OAuth2 access tokens
     *         taken over from the current authentication context.
     */
    RestClient.Builder createForTokenFromIncomingRequest();
}
