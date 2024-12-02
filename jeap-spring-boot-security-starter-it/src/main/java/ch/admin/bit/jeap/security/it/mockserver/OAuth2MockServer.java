package ch.admin.bit.jeap.security.it.mockserver;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.test.jws.JwsBuilder;
import ch.admin.bit.jeap.security.test.jws.RSAKeyUtils;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class OAuth2MockServer {

    private final int port;
    private final String basePath;
    private final WireMockServer wireMockServer;
    private final String configPath;
    private final String tokenPath;
    private final String jwksPath;
    private final RSAKey key;
    private final String issuer;

    public OAuth2MockServer(final int port, final String basePath) {
        this.port = port;
        this.basePath = basePath;
        this.wireMockServer = new WireMockServer(wireMockConfig().port(port));
        this.configPath = basePath + "/.well-known/openid-configuration";
        this.tokenPath = basePath + "/token";
        this.jwksPath = basePath + "/protocol/openid-connect/certs";
        this.key = RSAKeyUtils.createRsaKeyPair();
        this.issuer = "http://localhost:" + this.port + basePath;
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

    public String getIssuer() {
        return issuer;
    }

    public RSAKey getKey() {
        return key;
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
        final String portAndBasePath = port + basePath;
        return "{\n" +
                "  \"issuer\": \"http://localhost:" + portAndBasePath + "\",\n" +
                "  \"authorization_endpoint\": \"http://localhost:" + portAndBasePath + "/authorize\",\n" +
                "  \"token_endpoint\": \"http://localhost:" + portAndBasePath + "/token\",\n" +
                "  \"userinfo_endpoint\": \"http://localhost:" + portAndBasePath + "/userinfo\",\n" +
                "  \"end_session_endpoint\": \"http://localhost:" + portAndBasePath + "/logout\",\n" +
                "  \"jwks_uri\": \"http://localhost:" + portAndBasePath + "/.well-known/jwks.json\",\n" +
                "  \"introspection_endpoint\": \"http://localhost:" + portAndBasePath + "/protocol/openid-connect/token/introspect\",\n" +
                "  \"subject_types_supported\": [ \"public\", \"pairwise\" ]\n" +
                "}";
    }
}
