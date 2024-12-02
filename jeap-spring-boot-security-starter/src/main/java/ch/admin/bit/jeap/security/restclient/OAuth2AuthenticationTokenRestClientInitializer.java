package ch.admin.bit.jeap.security.restclient;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestInitializer;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;

public class OAuth2AuthenticationTokenRestClientInitializer implements ClientHttpRequestInitializer {

    @Override
    public void initialize(@NonNull ClientHttpRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof AbstractOAuth2Token token) {
            request.getHeaders().setBearerAuth(token.getTokenValue());
        }
    }
}