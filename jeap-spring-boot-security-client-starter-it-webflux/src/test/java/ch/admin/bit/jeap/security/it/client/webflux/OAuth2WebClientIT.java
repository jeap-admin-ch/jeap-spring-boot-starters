package ch.admin.bit.jeap.security.it.client.webflux;

import ch.admin.bit.jeap.security.client.JeapOAuth2WebclientBuilderFactory;
import ch.admin.bit.jeap.security.it.bearertoken.BearerTokenTestExtension;
import ch.admin.bit.jeap.security.it.bearertoken.BearerTokenUrl;
import ch.admin.bit.jeap.security.it.mockserver.OAuth2MockServer;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.nimbusds.jwt.JWT;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("client")
@ExtendWith(BearerTokenTestExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OAuth2WebClientIT {

    private static final int OAUTH_MOCK_SERVER_PORT = 9090;
    private static final String OAUTH_MOCK_SERVER_BASE_PATH = "/oauth";

    private static final String TEST_CLIENT_ID = "test-client";
    private static final String TEST_CLIENT_ID_USER_DEFINED_SCOPE = "test-client-with-user-defined-scope";
    private static final String DEFAULT_SCOPE = "openid";
    private static final String USER_DEFINED_SCOPE = "user-defined-scope";

    private static final OAuth2MockServer oauth2MockServer = new OAuth2MockServer(OAUTH_MOCK_SERVER_PORT, OAUTH_MOCK_SERVER_BASE_PATH);

    private String bearerTokenUrl;

    static {
        oauth2MockServer.start();
    }

    @BeforeEach
    void setup(@BearerTokenUrl String bearerTokenUrl) {
        this.bearerTokenUrl = bearerTokenUrl;
        oauth2MockServer.reset();
    }

    @AfterAll
    static void tearDown() {
        oauth2MockServer.stop();
    }

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    JeapOAuth2WebclientBuilderFactory jeapOAuth2WebclientBuilderFactory;

    @Test
    // This test loads a token that is not nearly expired into the Spring Security authorized client registry used by the WebClient instances.
    // That token would interfere with the other tests that rely on no valid token or only a nearly expired token being present.
    @DirtiesContext
    void testCreateForClientId_WhenOutsideOfAnHttpRequest_ThenTokenFetchedForClient() {
        final Duration tokenTimeToLive = Duration.ofMinutes(2);
        final JWT token = oauth2MockServer.stubTokenRequestWithTokenExpiringIn(tokenTimeToLive);

        final WebClient webClient = jeapOAuth2WebclientBuilderFactory.createForClientRegistryId(TEST_CLIENT_ID).build();
        final String response = callResource(webClient);

        oauth2MockServer.verifyTokenRequest(1);
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
        final String responseFirstRequest = callResource(webClient);
        oauth2MockServer.verifyTokenRequest(1);
        assertThat(responseFirstRequest).isEqualTo(token.serialize());

        // Fetch no new token on consecutive second request
        final String responseSecondRequest = callResource(webClient);
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
        callResource(webClient);
        oauth2MockServer.verifyTokenRequest(1);

        // Fetch no new token on consecutive second request
        callResource(webClient);
        oauth2MockServer.verifyTokenRequest(1); // still only the first token request happened

        // Let the token expire
        Thread.sleep(tokenTimeToLive.plusSeconds(5).toMillis());

        // Fetch new token on delayed third request
        callResource(webClient);
        oauth2MockServer.verifyTokenRequest(2);  // a second token request happened

        // Fetch no new token on consecutive fourth request
        callResource(webClient);
        oauth2MockServer.verifyTokenRequest(2); // still only the first and second token request happened
    }

    @Test
    void testCreateForClientId_WhenNoUserDefinedClientScopeDefined_ThenDefaultScopeApplied() {
        oauth2MockServer.stubTokenRequestWithNearlyExpiredToken();

        callResource(jeapOAuth2WebclientBuilderFactory.createForClientRegistryId(TEST_CLIENT_ID).build());

        oauth2MockServer.verifyTokenRequestBody(matchingScope(DEFAULT_SCOPE));
    }

    @Test
    void testCreateForClientId_WhenUserDefinedClientScopeDefined_ThenUserDefinedScopeApplied() {
        oauth2MockServer.stubTokenRequestWithNearlyExpiredToken();

        callResource(jeapOAuth2WebclientBuilderFactory.createForClientRegistryId(TEST_CLIENT_ID_USER_DEFINED_SCOPE).build());

        oauth2MockServer.verifyTokenRequestBody(matchingScope(USER_DEFINED_SCOPE));
    }

    @Test
    void testCreateForClientId_WhenClientNotConfiguredThenThrowsException() {
        assertThatThrownBy( () -> jeapOAuth2WebclientBuilderFactory.createForClientRegistryId("unknown") ).
                isInstanceOf(IllegalArgumentException.class).
                hasMessageContaining("There is no client registration with id 'unknown' configured.");
    }

    private StringValuePattern matchingScope(String scope) {
        return matching("(^|.+&)scope=" + scope + "(&.+|$)");
    }

    private String callResource(WebClient webClient) {
        return webClient.get().
                uri(bearerTokenUrl).
                retrieve().
                bodyToMono(String.class).
                block(Duration.ofSeconds(10));
    }

}
