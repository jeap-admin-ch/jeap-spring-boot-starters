package ch.admin.bit.jeap.security.client;

import lombok.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Objects;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@SuppressWarnings("removal")
public class DefaultJeapOAuth2WebclientBuilderFactory implements JeapOAuth2WebclientBuilderFactory {

    private final WebClient.Builder webClientBuilder;
    private final ExchangeFilterFunction oauth2ClientExchangeFilterFunction;
    private final ExchangeFilterFunction bearerFromAuthenticationExchangeFilterFunction;
    private final ClientRegistrationResolver clientRegistrationResolver;

    public DefaultJeapOAuth2WebclientBuilderFactory(WebClient.Builder webClientBuilder,
                                                    @NonNull ExchangeFilterFunction oauth2ClientExchangeFilterFunction,
                                                    @NonNull ExchangeFilterFunction bearerFromAuthenticationExchangeFilterFunction,
                                                    @NonNull ClientRegistrationResolver clientRegistrationResolver) {
        this.webClientBuilder = Objects.requireNonNull(webClientBuilder).clone();
        this.oauth2ClientExchangeFilterFunction = oauth2ClientExchangeFilterFunction;
        this.bearerFromAuthenticationExchangeFilterFunction = bearerFromAuthenticationExchangeFilterFunction;
        this.clientRegistrationResolver = clientRegistrationResolver;
    }

    public DefaultJeapOAuth2WebclientBuilderFactory(WebClient.Builder webClientBuilder,
                                                    @NonNull ExchangeFilterFunction bearerFromAuthenticationExchangeFilterFunction) {
        this.webClientBuilder = Objects.requireNonNull(webClientBuilder).clone();
        this.bearerFromAuthenticationExchangeFilterFunction = bearerFromAuthenticationExchangeFilterFunction;
        this.oauth2ClientExchangeFilterFunction = null;
        this.clientRegistrationResolver = null;
    }


    @Override
    public WebClient.Builder createForClientId(String clientRegistryId) {
        return createForClientRegistryId(clientRegistryId);
    }

    @Override
    public WebClient.Builder createForClientRegistryId(String clientRegistryId) {
        assertOAuth2ClientConfigured(clientRegistryId);
        return webClientBuilder.clone().
                // Make the client id for the OAauth2 exchange filter function known
                filter((request, next) -> next.exchange(ClientRequest.from(request).attributes(clientRegistrationId(clientRegistryId)).build())).
                // Enable OAuth2 bearer token population on exchanges
                filter(oauth2ClientExchangeFilterFunction);
    }

    @Override
    public WebClient.Builder createForClientIdPreferringTokenFromIncomingRequest(String clientRegistryId) {
        return createForClientRegistryIdPreferringTokenFromIncomingRequest(clientRegistryId);
    }

    @Override
    public WebClient.Builder createForClientRegistryIdPreferringTokenFromIncomingRequest(String clientRegistryId) {
        assertOAuth2ClientConfigured(clientRegistryId);
        return webClientBuilder.clone().
            // Make the client id for the OAauth2 exchange filter function known
            filter((request, next) -> next.exchange(ClientRequest.from(request).attributes(clientRegistrationId(clientRegistryId)).build())).
            // Use token from current authentication if available
            filter(bearerFromAuthenticationExchangeFilterFunction).
            // Use token from the configured OAuth2 client if there was no token available from the current authentication
            filter((request, next) -> {
                if (!request.headers().containsKey(HttpHeaders.AUTHORIZATION)) {
                    return oauth2ClientExchangeFilterFunction.filter(request, next);
                }
                else {
                    return next.exchange(request);
                }
            });
    }

    @Override
    public WebClient.Builder createForTokenFromIncomingRequest() {
        return webClientBuilder.clone().
                filter(bearerFromAuthenticationExchangeFilterFunction);
    }

    private void assertOAuth2ClientConfigured(String clientRegistryId) {
        if (oauth2ClientExchangeFilterFunction == null) {
            throw new UnsupportedOperationException("Application not configured as OAuth2 client.");
        }
        if (clientRegistrationResolver.findByRegistrationId(clientRegistryId) == null) {
            throw new IllegalArgumentException("There is no client registration with id '" + clientRegistryId + "' configured.");
        }
    }

    @FunctionalInterface
    public interface ClientRegistrationResolver {
        ClientRegistration findByRegistrationId(String id);
    }

}
