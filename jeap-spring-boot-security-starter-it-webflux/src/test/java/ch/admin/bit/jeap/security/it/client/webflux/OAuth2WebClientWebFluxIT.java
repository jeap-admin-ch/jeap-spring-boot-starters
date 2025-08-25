package ch.admin.bit.jeap.security.it.client.webflux;

import ch.admin.bit.jeap.security.client.JeapOAuth2WebclientBuilderFactory;
import ch.admin.bit.jeap.security.it.bearertoken.BearerTokenTestExtension;
import ch.admin.bit.jeap.security.it.bearertoken.BearerTokenUrl;
import ch.admin.bit.jeap.security.it.mockserver.OAuth2MockServer;
import ch.admin.bit.jeap.security.it.resource.OAuth2TestGateway;
import ch.admin.bit.jeap.security.it.resource.OAuth2TestGateway.WebClientTokenSource;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import com.nimbusds.jwt.JWT;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static ch.admin.bit.jeap.security.it.resource.OAuth2TestGateway.WebClientTokenSource.CLIENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("client")
@ExtendWith(BearerTokenTestExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OAuth2WebClientWebFluxIT {

    private static final int OAUTH_MOCK_SERVER_PORT = 9090;
    private static final String OAUTH_MOCK_SERVER_BASE_PATH = "/oauth";

    private static final String TEST_CLIENT_ID = "test-client";

    private static final OAuth2MockServer oauth2MockServer = new OAuth2MockServer(OAUTH_MOCK_SERVER_PORT, OAUTH_MOCK_SERVER_BASE_PATH);

    private static String bearerTokenUrl;

    @LocalServerPort
    int serverPort;

    @Autowired
    JeapOAuth2WebclientBuilderFactory jeapOAuth2WebclientBuilderFactory;

    static {
        oauth2MockServer.start();
    }

    @BeforeAll
    static void init(@BearerTokenUrl String url) {
        bearerTokenUrl = url;
    }

    @BeforeEach
    void setup() {
        oauth2MockServer.reset();
    }

    @AfterAll
    static void tearDown() {
        oauth2MockServer.stop();
    }

    @Test
    void testCreateForClientId_WhenOutsideOfAnHttpRequest_ThenTokenFetchedForClient() {
        final JWT token = oauth2MockServer.stubTokenRequestWithNearlyExpiredToken();

        final WebClient webClient = jeapOAuth2WebclientBuilderFactory.createForClientRegistryId(TEST_CLIENT_ID).build();
        final String response = callResource(webClient).block(Duration.ofSeconds(10));

        oauth2MockServer.verifyTokenRequest(2); // If a client is given a nearly expired token, Spring Security for WebFlux seems to fetch a second token in the hope that it would not expire as soon as the first one.
        assertThat(response).isEqualTo(token.serialize());
    }

    @Test
    void testCreateForClientId_WhenInsideOfAnHttpRequest_ThenTokenFetchedForClient(@Autowired WebClient.Builder webClientBuilder) {
        final JWT token = oauth2MockServer.stubTokenRequestWithNearlyExpiredToken();

        final WebClient webClient = webClientBuilder.clone().build();
        final String response = callGateway(webClient, CLIENT).block(Duration.ofSeconds(10));

        oauth2MockServer.verifyTokenRequest(2); // If a client is given a nearly expired token, Spring Security for WebFlux seems to fetch a second token in the hope that it would not expire as soon as the first one.
        assertThat(response).isEqualTo(token.serialize());
    }

    @Test
    void testCreateForTokenFromIncomingRequest_WhenAuthenticated_ThenTokenFromAuthenticationUsed() {
        JeapAuthenticationToken authentication = defaultAuthentication();

        final WebClient webClient = jeapOAuth2WebclientBuilderFactory.createForTokenFromIncomingRequest().build();
        final String response = callResource(webClient).
                          contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)).
                          block(Duration.ofSeconds(10));

        oauth2MockServer.verifyTokenRequest(0);
        assertThat(response).isEqualTo(authentication.getToken().getTokenValue());
    }

    @Test
    void testCreateForTokenFromIncomingRequest_WhenNotAuthenticated_ThenNoTokenUsed() {
        final WebClient webClient = jeapOAuth2WebclientBuilderFactory.createForTokenFromIncomingRequest().build();
        final String response = callResource(webClient).block(Duration.ofSeconds(10));

        oauth2MockServer.verifyTokenRequest(0);
        assertThat(response).isNullOrEmpty();
    }

    @Test
    void testForClientIdPreferringTokenFromIncomingRequest_WhenAuthenticated_ThenTokenFromAuthenticationUsed() {
        final JeapAuthenticationToken authentication = defaultAuthentication();

        final WebClient webClient = jeapOAuth2WebclientBuilderFactory.createForClientRegistryIdPreferringTokenFromIncomingRequest(TEST_CLIENT_ID).build();
        final String response = callResource(webClient).
                          contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)).
                          block(Duration.ofSeconds(10));

        oauth2MockServer.verifyTokenRequest(0);
        assertThat(response).isEqualTo(authentication.getToken().getTokenValue());
    }

    @Test
    void testForClientIdPreferringTokenFromIncomingRequest_WhenNotAuthenticated_ThenTokenFetchedForClient() {
        final JWT token = oauth2MockServer.stubTokenRequestWithNearlyExpiredToken();

        final WebClient webClient = jeapOAuth2WebclientBuilderFactory.createForClientRegistryIdPreferringTokenFromIncomingRequest(TEST_CLIENT_ID).build();
        final String response = callResource(webClient).block(Duration.ofSeconds(10));

        oauth2MockServer.verifyTokenRequest(2); // If a client is given a nearly expired token, Spring Security for WebFlux seems to fetch a second token in the hope that it would not expire as soon as the first one.
        assertThat(response).isEqualTo(token.serialize());
    }

    @Test
    // This test loads a token that is not nearly expired into the Spring Security authorized client registry used by the WebClient instances.
    // That token would interfere with the other tests that rely on no valid token or only a nearly expired token being present.
    @DirtiesContext
    void testCreateForClientId_WhenTwoConsecutiveRequests_ThenOnlyOneTokenFetched() {
        final Duration tokenTimeToLive = Duration.ofMinutes(2);
        final JWT token = oauth2MockServer.stubTokenRequestWithTokenExpiringIn(tokenTimeToLive);
        final WebClient webClient = jeapOAuth2WebclientBuilderFactory.createForClientRegistryId(TEST_CLIENT_ID).build();

        // Fetch new token on first request
        final String responseFirstRequest = callResource(webClient).block(Duration.ofSeconds(10));
        oauth2MockServer.verifyTokenRequest(1);
        assertThat(responseFirstRequest).isEqualTo(token.serialize());

        // Fetch no new token on consecutive second request
        final String responseSecondRequest = callResource(webClient).block(Duration.ofSeconds(10));
        oauth2MockServer.verifyTokenRequest(1); // still only the first token request happened
        assertThat(responseSecondRequest).isEqualTo(token.serialize()); // still the same token
    }


    @Test
    @Disabled("This test has to wait some minutes to complete. We don't want to wait this long for every build. Rerun this test manually before a release.")
    void testCreateForClientId_WhenTokenExpires_ThenFetchNewToken() throws Exception {
        final Duration tokenTimeToLive = Duration.ofMinutes(2);
        oauth2MockServer.stubTokenRequestWithTokenExpiringIn(tokenTimeToLive);
        final WebClient webClient = jeapOAuth2WebclientBuilderFactory.createForClientRegistryId(TEST_CLIENT_ID).build();

        // Fetch new token on first request
        callResource(webClient).block(Duration.ofSeconds(10));
        oauth2MockServer.verifyTokenRequest(1);

        // Fetch no new token on consecutive second request
        callResource(webClient).block(Duration.ofSeconds(10));
        oauth2MockServer.verifyTokenRequest(1); // still only the first token request happened

        // Let the token expire
        Thread.sleep(tokenTimeToLive.plusSeconds(5).toMillis());

        // Fetch new token on delayed third request
        callResource(webClient).block(Duration.ofSeconds(10));
        oauth2MockServer.verifyTokenRequest(2);  // a second token request happened

        // Fetch no new token on consecutive fourth request
        callResource(webClient).block(Duration.ofSeconds(10));
        oauth2MockServer.verifyTokenRequest(2); // still only the first and second token request happened
    }

    @Test
    void testCreateForClientId_WhenClientNotConfigured_ThenThrowsException() {
        assertThatThrownBy( () -> jeapOAuth2WebclientBuilderFactory.createForClientRegistryId("unknown") ).
                isInstanceOf(IllegalArgumentException.class).
                hasMessageContaining("There is no client registration with id 'unknown' configured.");
    }

    @Test
    void testCreateForClientIdPreferringTokenFromIncomingRequest_WhenClientNotConfigured_ThenThrowsException() {
        assertThatThrownBy( () -> jeapOAuth2WebclientBuilderFactory.createForClientRegistryIdPreferringTokenFromIncomingRequest("unknown") ).
                isInstanceOf(IllegalArgumentException.class).
                hasMessageContaining("There is no client registration with id 'unknown' configured.");
    }

    private Mono<String> callResource(WebClient webClient) {
        return webClient.get().
                uri(bearerTokenUrl).
                retrieve().
                bodyToMono(String.class);
    }

    private Mono<String> callGateway(WebClient webClient, WebClientTokenSource tokenSource) {
        return webClient.get().
                uri("http://localhost:" + serverPort + OAuth2TestGateway.API_PATH,
                        uriBuilder -> uriBuilder.queryParam(OAuth2TestGateway.TOKEN_SOURCE_PARAM_NAME, tokenSource).build()).
                retrieve().
                bodyToMono(String.class);
    }

    private JeapAuthenticationToken defaultAuthentication() {
        return  JeapAuthenticationTestTokenBuilder.create().withContext(JeapAuthenticationContext.SYS).build();
    }

    @TestConfiguration
    static class GatewayConfig {
        @Bean
        OAuth2TestGateway oAuth2TestGateway(JeapOAuth2WebclientBuilderFactory jeapOAuth2WebclientBuilderFactory) {
            return new OAuth2TestGateway(jeapOAuth2WebclientBuilderFactory, bearerTokenUrl);
        }
    }

}
