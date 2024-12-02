package ch.admin.bit.jeap.security.restclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.*;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

@Slf4j
public class OAuth2ClientCredentialsRestClientInitializer implements ClientHttpRequestInitializer {

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final ClientRegistration clientRegistration;

    private final boolean preferTokenFromIncomingRequest;

    public OAuth2ClientCredentialsRestClientInitializer(OAuth2AuthorizedClientManager authorizedClientManager, ClientRegistration clientRegistration, boolean preferTokenFromIncomingRequest) {
        this.authorizedClientManager = authorizedClientManager;
        this.clientRegistration = clientRegistration;
        this.preferTokenFromIncomingRequest = preferTokenFromIncomingRequest;
    }

    @Override
    public void initialize(@NonNull ClientHttpRequest request) {
        if (preferTokenFromIncomingRequest && request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            log.trace("The configuration preferTokenFromIncomingRequest is true: reusing the token from incoming request...");
        } else {
            final String clientRegistrationId = this.clientRegistration.getRegistrationId();
            final OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId(clientRegistrationId)
                    .principal(this.clientRegistration.getClientId())
                    .build();
            final OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
            if (authorizedClient == null) {
                throw new IllegalStateException("client credentials flow on " + clientRegistrationId + " failed, client is null");
            }
            request.getHeaders().setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
        }
    }
}