package ch.admin.bit.jeap.security.test.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestInitializer;
import org.springframework.lang.NonNull;

@Slf4j
public class OAuth2ClientAuthTokenProviderRestClientInitializer implements ClientHttpRequestInitializer {

    private final AuthTokenProvider authTokenProvider;

    public OAuth2ClientAuthTokenProviderRestClientInitializer(AuthTokenProvider authTokenProvider) {
        this.authTokenProvider = authTokenProvider;
    }

    @Override
    public void initialize(@NonNull ClientHttpRequest request) {
            String authToken = authTokenProvider.getAuthToken();
            request.getHeaders().setBearerAuth(authToken);
    }
}