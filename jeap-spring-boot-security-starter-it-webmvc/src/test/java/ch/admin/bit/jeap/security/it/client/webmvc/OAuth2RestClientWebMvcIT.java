package ch.admin.bit.jeap.security.it.client.webmvc;

import ch.admin.bit.jeap.security.it.mockserver.OAuth2MockServer;
import ch.admin.bit.jeap.security.it.resource.BearerTokenResource;
import ch.admin.bit.jeap.security.it.resource.OAuth2TestGateway;
import ch.admin.bit.jeap.security.it.resource.OAuth2TestGateway.WebClientTokenSource;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.restclient.JeapOAuth2RestClientBuilderFactory;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.nimbusds.jwt.JWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

import static ch.admin.bit.jeap.security.it.resource.OAuth2TestGateway.WebClientTokenSource.CLIENT;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ActiveProfiles("client")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class OAuth2RestClientWebMvcIT {

    private static final int OAUTH_MOCK_SERVER_PORT = 9090;
    private static final String OAUTH_MOCK_SERVER_BASE_PATH = "/oauth";

    private static final String TEST_CLIENT_ID = "test-client";
    private static final String TEST_CLIENT_ID_USER_DEFINED_SCOPE = "test-client-with-user-defined-scope";
    private static final String DEFAULT_SCOPE = "openid";
    private static final String USER_DEFINED_SCOPE = "user-defined-scope";

    private static final OAuth2MockServer oauth2MockServer = new OAuth2MockServer(OAUTH_MOCK_SERVER_PORT, OAUTH_MOCK_SERVER_BASE_PATH);

    @Mock
    private ServletRequestAttributes attrs;
    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    private HttpServletResponse servletResponse;
    @Mock
    private HttpSession httpSession;

    static {
        oauth2MockServer.start();
    }

    @BeforeEach
    void setup(){
        when(attrs.getRequest()).thenReturn(servletRequest);
        when(servletRequest.getSession()).thenReturn(httpSession);
        when(attrs.getResponse()).thenReturn(servletResponse);
        RequestContextHolder.setRequestAttributes(attrs);
    }

    @AfterEach
    void reset() {
        oauth2MockServer.reset();
    }

    @AfterAll
    static void tearDown() {
        oauth2MockServer.stop();
    }

    @Value("${server.port}")
    int serverPort;

    @Autowired
    JeapOAuth2RestClientBuilderFactory jeapOAuth2RestClientBuilderFactory;

    @Test
    void testCreateForClientId_WhenOutsideOfAnHttpRequest_ThenTokenFetchedForClient() {
        final JWT token = oauth2MockServer.stubTokenRequestWithNearlyExpiredToken();

        final RestClient restClient = jeapOAuth2RestClientBuilderFactory.createForClientRegistryId(TEST_CLIENT_ID).build();
        final String response = callResource(restClient);

        oauth2MockServer.verifyTokenRequest(1);
        assertThat(response).isEqualTo(token.serialize());
    }

    @Test
    void testCreateForClientId_WhenInsideOfAnHttpRequest_ThenTokenFetchedForClient(@Autowired RestClient.Builder restClientBuilder) {
        final JWT token = oauth2MockServer.stubTokenRequestWithNearlyExpiredToken();

        final RestClient restClient = restClientBuilder.clone().build();
        final String response = callGateway(restClient, CLIENT);

        oauth2MockServer.verifyTokenRequest(1);
        assertThat(response).isEqualTo(token.serialize());
    }

    @Test
    @WithAuthentication("defaultAuthentication")
    void testCreateForTokenFromIncomingRequest_WhenAuthenticated_ThenTokenFromAuthenticationUsed() {
        final String authToken = ((JeapAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken().getTokenValue();

        final RestClient restClient = jeapOAuth2RestClientBuilderFactory.createForTokenFromIncomingRequest().build();
        final String response = callResource(restClient);

        oauth2MockServer.verifyTokenRequest(0);
        assertThat(response).isEqualTo(authToken);
    }

    @Test
    void testCreateForTokenFromIncomingRequest_WhenNotAuthenticated_ThenNoTokenUsed() {
        final RestClient restClient = jeapOAuth2RestClientBuilderFactory.createForTokenFromIncomingRequest().build();
        final String response = callResource(restClient);

        oauth2MockServer.verifyTokenRequest(0);
        assertThat(response).isNullOrEmpty();
    }

    @Test
    @WithAuthentication("defaultAuthentication")
    void testForClientIdPreferringTokenFromIncomingRequest_WhenAuthenticated_ThenTokenFromAuthenticationUsed() {
        final String authToken = ((JeapAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken().getTokenValue();

        final RestClient restClient = jeapOAuth2RestClientBuilderFactory.createForClientRegistryIdPreferringTokenFromIncomingRequest(TEST_CLIENT_ID).build();
        final String response = callResource(restClient);

        oauth2MockServer.verifyTokenRequest(0);
        assertThat(response).isEqualTo(authToken);
    }

    @Test
    void testForClientIdPreferringTokenFromIncomingRequest_WhenNotAuthenticated_ThenTokenFetchedForClient() {
        final JWT token = oauth2MockServer.stubTokenRequestWithNearlyExpiredToken();

        final RestClient restClient = jeapOAuth2RestClientBuilderFactory.createForClientRegistryIdPreferringTokenFromIncomingRequest(TEST_CLIENT_ID).build();
        final String response = callResource(restClient);

        oauth2MockServer.verifyTokenRequest(1);
        assertThat(response).isEqualTo(token.serialize());
    }

    @Test
    // This test loads a token that is not nearly expired into the Spring Security authorized client registry used by the RestClient instances.
    // That token would interfere with the other tests that rely on no valid token or only a nearly expired token being present.
    @DirtiesContext
    void testCreateForClientId_WhenTwoConsecutiveRequests_ThenOnlyOneTokenFetched() {
        final Duration tokenTimeToLive = Duration.ofMinutes(2);
        final JWT token = oauth2MockServer.stubTokenRequestWithTokenExpiringIn(tokenTimeToLive);
        final RestClient restClient = jeapOAuth2RestClientBuilderFactory.createForClientRegistryId(TEST_CLIENT_ID).build();

        // Fetch new token on first request
        final String responseFirstRequest = callResource(restClient);
        oauth2MockServer.verifyTokenRequest(1);
        assertThat(responseFirstRequest).isEqualTo(token.serialize());

        // Fetch no new token on consecutive second request
        final String responseSecondRequest = callResource(restClient);
        oauth2MockServer.verifyTokenRequest(1); // still only the first token request happened
        assertThat(responseSecondRequest).isEqualTo(token.serialize()); // still the same token
    }

    @Test
    @Disabled("This test has to wait some minutes to complete. We don't want to wait this long for every build. Rerun this test manually before a release.")
    void testCreateForClientId_WhenTokenExpires_ThenFetchNewToken() throws Exception {
        final Duration tokenTimeToLive = Duration.ofMinutes(2);
        oauth2MockServer.stubTokenRequestWithTokenExpiringIn(tokenTimeToLive);
        final RestClient restClient = jeapOAuth2RestClientBuilderFactory.createForClientRegistryId(TEST_CLIENT_ID).build();

        // Fetch new token on first request
        callResource(restClient);
        oauth2MockServer.verifyTokenRequest(1);

        // Fetch no new token on consecutive second request
        callResource(restClient);
        oauth2MockServer.verifyTokenRequest(1); // still only the first token request happened

        // Let the token expire
        Thread.sleep(tokenTimeToLive.plusSeconds(5).toMillis());

        // Fetch new token on delayed third request
        callResource(restClient);
        oauth2MockServer.verifyTokenRequest(2);  // a second token request happened

        // Fetch no new token on consecutive fourth request
        callResource(restClient);
        oauth2MockServer.verifyTokenRequest(2); // still only the first and second token request happened
    }

    @Test
    void testCreateForClientId_WhenNoUserDefinedClientScopeDefined_ThenDefaultScopeApplied() {
        oauth2MockServer.stubTokenRequestWithNearlyExpiredToken();

        callResource(jeapOAuth2RestClientBuilderFactory.createForClientRegistryId(TEST_CLIENT_ID).build());

        oauth2MockServer.verifyTokenRequestBody(matchingScope(DEFAULT_SCOPE));
    }

    @Test
    void testCreateForClientId_WhenUserDefinedClientScopeDefined_ThenUserDefinedScopeApplied() {
        oauth2MockServer.stubTokenRequestWithNearlyExpiredToken();

        callResource(jeapOAuth2RestClientBuilderFactory.createForClientRegistryId(TEST_CLIENT_ID_USER_DEFINED_SCOPE).build());

        oauth2MockServer.verifyTokenRequestBody(matchingScope(USER_DEFINED_SCOPE));
    }

    @Test
    void testCreateForClientId_WhenClientNotConfigured_ThenThrowsException() {
        assertThatThrownBy( () -> jeapOAuth2RestClientBuilderFactory.createForClientRegistryId("unknown") ).
                isInstanceOf(IllegalArgumentException.class).
                hasMessageContaining("There is no client registration with id 'unknown' configured.");
    }

    @Test
    void testCreateForClientRegistryIdPreferringTokenFromIncomingRequest_WhenClientNotConfigured_ThenThrowsException() {
        assertThatThrownBy( () -> jeapOAuth2RestClientBuilderFactory.createForClientRegistryIdPreferringTokenFromIncomingRequest("unknown") ).
                isInstanceOf(IllegalArgumentException.class).
                hasMessageContaining("There is no client registration with id 'unknown' configured.");
    }

    private StringValuePattern matchingScope(String scope) {
        return matching("(^|.+&)scope=" + scope + "(&.+|$)");
    }

    private String callResource(RestClient restClient) {
        return restClient.get().
                uri("http://localhost:" + serverPort + BearerTokenResource.API_PATH).
                retrieve().
                body(String.class);
    }

    private String callGateway(RestClient restClient, WebClientTokenSource tokenSource) {
        return restClient.get().
                uri("http://localhost:" + serverPort + OAuth2TestGateway.API_PATH,
                        uriBuilder -> uriBuilder.queryParam(OAuth2TestGateway.TOKEN_SOURCE_PARAM_NAME, tokenSource).build()).
                retrieve().
                body(String.class);
    }

    // method appears to be unused but is required for authentication factory.
    private JeapAuthenticationToken defaultAuthentication() {
        return  JeapAuthenticationTestTokenBuilder.create().withContext(JeapAuthenticationContext.SYS).build();
    }
}
