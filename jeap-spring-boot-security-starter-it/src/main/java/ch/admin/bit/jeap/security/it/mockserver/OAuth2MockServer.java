package ch.admin.bit.jeap.security.it.mockserver;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.test.jws.JwsBuilder;
import ch.admin.bit.jeap.security.test.jws.RSAKeyUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.Getter;
import lombok.SneakyThrows;

import java.text.ParseException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class OAuth2MockServer {

    private static final String DEFAULT_JWKS_PATH = "/protocol/openid-connect/certs";
    private static final String DEFAULT_CONFIG_PATH = "/.well-known/openid-configuration";
    private static final String DEFAULT_TOKEN_PATH = "/token";
    private static final String DEFAULT_INTROSPECTION_PATH = "/token/introspect";
    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";

    private final String basePath;
    private final WireMockServer wireMockServer;
    private final String configPath;
    private final String tokenPath;
    private final String jwksPath;
    private final String introspectionPath;
    @Getter
    private final RSAKey key;

    // user port = 0 for random port
    public OAuth2MockServer(final int port, final String basePath) {
        this.basePath = basePath;
        this.wireMockServer = new WireMockServer(wireMockConfig().port(port));
        this.configPath = basePath + DEFAULT_CONFIG_PATH;
        this.tokenPath = basePath + DEFAULT_TOKEN_PATH;
        this.jwksPath = basePath + DEFAULT_JWKS_PATH;
        this.introspectionPath = basePath + DEFAULT_INTROSPECTION_PATH;
        this.key = RSAKeyUtils.createRsaKeyPair();
    }

    public void start() {
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        stubConfigRequest();
        stubJwksRequest();
    }

    public void stop() {
        wireMockServer.stop();
    }

    public void reset() {
        wireMockServer.resetAll();
        stubConfigRequest();
    }

    public String getBaseUrl() {
        return wireMockServer.baseUrl();
    }

    public String getIssuer() {
        return getBaseUrl() + basePath;
    }

    public String getIntrospectionUri() {
        return getIssuer() + DEFAULT_INTROSPECTION_PATH;
    }

    /**
     * Make the authorization mock respond with a short lived token on a token request.
     * Stubbing with a token that expires in less than a minute makes Spring Security use the token only once
     * for the webclient call that fetched the token and then for the next webclient call fetch a new token again.
     *
     * @return The token that the authorization mock will respond with on a token request.
     */
    public JWT stubTokenRequestWithNearlyExpiredToken() {
        return stubTokenRequestWithTokenExpiringIn(Duration.ofSeconds(30));
    }

    public JWT stubTokenRequestWithTokenExpiringIn(Duration tokenExpiresIn) {
        JWT jwt = createToken(tokenExpiresIn);
        stubTokenRequestWithToken(jwt);
        return jwt;
    }

    public void stubTokenRequestWithToken(JWT token) {
        stubFor(post(tokenPath).willReturn(okJson(getOAuthMockServerTokenBody(token))));
    }

    @SneakyThrows
    public void stubTokenIntrospectionRequest(JWT token, boolean tokenActive, Map<String, Object> additionalClaims) {
        Map<String, Object> introspectionAttributes = new HashMap<>(token.getJWTClaimsSet().getClaims());
        introspectionAttributes.put("active", tokenActive);
        introspectionAttributes.put("introspected", "only-on-introspection");
        Optional.ofNullable(additionalClaims).ifPresent(introspectionAttributes::putAll);
        String introspectionResponse = new ObjectMapper().writeValueAsString(introspectionAttributes);
        stubFor(post(introspectionPath)
                .withBasicAuth(CLIENT_ID, CLIENT_SECRET)
                .withRequestBody(containing("token=" + token.serialize()))
                .willReturn(okJson(introspectionResponse)));
    }

    @SneakyThrows
    public void stubTokenIntrospectionRequestWithDelayedResponse(JWT token, int delayInMilliseconds) {
        Map<String, Object> introspectionAttributes = new HashMap<>(token.getJWTClaimsSet().getClaims());
        introspectionAttributes.put("active", true);
        String introspectionResponse = new ObjectMapper().writeValueAsString(introspectionAttributes);
        stubFor(post(introspectionPath)
                .withBasicAuth(CLIENT_ID, CLIENT_SECRET)
                .withRequestBody(containing("token=" + token.serialize()))
                .willReturn(okJson(introspectionResponse).withFixedDelay(delayInMilliseconds))
        );
    }

    public void stubTokenIntrospectionErrorResponseRequest() {
        stubFor(post(introspectionPath).willReturn(status(400).withBody("{\"error\": \"invalid_request\"}")));
    }

    private void stubConfigRequest() {
        stubFor(get(configPath).willReturn(okJson(getOAuthMockServerConfigBody())));
    }

    private void stubJwksRequest() {
        stubFor(get(jwksPath).willReturn(okJson(getOAuthMockServerJwksBody())));
    }

    public SignedJWT createToken(Duration tokenExpiresIn) {
        String id = UUID.randomUUID().toString();
        String issuer = getIssuer();
        ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        ZonedDateTime expiry = now.plus(tokenExpiresIn);
        String subject = "mock";
        return JwsBuilder.create(id, issuer, expiry, now, now, subject, JeapAuthenticationContext.SYS)
                .withRsaKey(key)
                .build();
    }

    public void verifyTokenRequest(int times) {
        verify(exactly(times), postRequestedFor(urlEqualTo(tokenPath)));
    }

    public void verifyTokenRequestBody(ContentPattern<?> contentPattern) {
        verify(postRequestedFor(urlEqualTo(tokenPath)).withRequestBody(contentPattern));
    }

    public void verifyTokenIntrospectionRequest(int times) {
        verify(exactly(times), postRequestedFor(urlEqualTo(introspectionPath)));
    }

    private String getOAuthMockServerJwksBody() {
        return new JWKSet(key).toString(); // does not include private key
    }

    private String getOAuthMockServerTokenBody(JWT jwt) {
        JWTClaimsSet claims;
        try {
            claims = jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unparsable JWT.", e);
        }
        long expiresIn = Math.max((claims.getExpirationTime().getTime() - (new Date()).getTime()) / 1000, 0);
        return "{\n" +
                "    \"access_token\": \"" + jwt.serialize() + "\",\n" +
                "    \"token_type\": \"bearer\",\n" +
                "    \"expires_in\": " + expiresIn + ",\n" +
                "    \"scope\": \"offline_access openid profile roles web-origins\",\n" +
                "    \"jti\": \"" + claims.getJWTID() + "\"\n" +
                "}";
    }

    private String getOAuthMockServerConfigBody() {
        final String issuer = getIssuer();
        return "{\n" +
                "  \"issuer\": \"" + issuer + "\",\n" +
                "  \"authorization_endpoint\": \"" + issuer + "/authorize\",\n" +
                "  \"token_endpoint\": \"" + issuer + DEFAULT_TOKEN_PATH + "\",\n" +
                "  \"userinfo_endpoint\": \"" + issuer + "/userinfo\",\n" +
                "  \"end_session_endpoint\": \"" + issuer + "/logout\",\n" +
                "  \"jwks_uri\": \"" + issuer + DEFAULT_JWKS_PATH + "\",\n" +
                "  \"introspection_endpoint\": \"" + getIntrospectionUri() + "\",\n" +
                "  \"subject_types_supported\": [ \"public\", \"pairwise\" ]\n" +
                "}";
    }
}
