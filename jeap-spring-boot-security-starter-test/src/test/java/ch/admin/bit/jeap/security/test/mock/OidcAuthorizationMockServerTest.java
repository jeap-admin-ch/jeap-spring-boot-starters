package ch.admin.bit.jeap.security.test.mock;

import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OidcAuthorizationMockServerTest {

    @Test
    @SuppressWarnings("unchecked")
    void authorizationCodeFlowWithPkce() throws IOException, InterruptedException, ParseException {
        int port = freePort();
        String basePath = "/mock-idp";
        String issuer = "http://localhost:" + port + basePath;
        String clientId = "test-client";
        String nonce = "nonce-123";
        String state = "state-xyz";
        String redirectUri = "http://localhost/callback";

        OidcAuthorizationMockServer server = OidcAuthorizationMockServer.builder(port, basePath, "http://localhost:8080")
                .withDefaultClientId(clientId)
                .withSubject("test-user")
                .withPreferredUsername("test-user")
                .withEmail("test-user@example.org")
                .withScope("openid profile roles")
                .withUserRoles(List.of("jeap_@system_#read"))
                .withAccessTokenClaims(Map.of("custom_access", "a1"))
                .withIdTokenClaims(Map.of("custom_id", "i1"))
                .withUserInfoClaims(Map.of("department", "IT"))
                .build();

        HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

        try {
            server.start();

            HttpResponse<String> discovery = httpClient.send(
                    HttpRequest.newBuilder(URI.create(issuer + "/.well-known/openid-configuration")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(discovery.statusCode()).isEqualTo(200);
            Map<String, Object> discoveryJson = parseJson(discovery.body());
            assertThat(discoveryJson)
                    .containsEntry("issuer", issuer)
                    .containsEntry("token_endpoint", issuer + "/oauth2/token")
                    .containsEntry("jwks_uri", issuer + "/.well-known/jwks.json");

            String codeVerifier = "my-very-secret-code-verifier";
            String codeChallenge = s256(codeVerifier);
            String authorizeUrl = issuer + "/oauth2/authorize"
                    + "?redirect_uri=" + enc(redirectUri)
                    + "&state=" + enc(state)
                    + "&nonce=" + enc(nonce)
                    + "&code_challenge=" + enc(codeChallenge)
                    + "&code_challenge_method=S256";

            HttpResponse<String> authorize = httpClient.send(
                    HttpRequest.newBuilder(URI.create(authorizeUrl)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(authorize.statusCode()).isEqualTo(302);
            String location = authorize.headers().firstValue("Location").orElseThrow();
            assertThat(location).contains("code=");
            assertThat(location).contains("state=" + state);
            String code = extractQueryParam(location, "code");

            String form = "grant_type=authorization_code"
                    + "&code=" + enc(code)
                    + "&code_verifier=" + enc(codeVerifier)
                    + "&client_id=" + enc(clientId);

            HttpResponse<String> token = httpClient.send(
                    HttpRequest.newBuilder(URI.create(issuer + "/oauth2/token"))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(form))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(token.statusCode()).isEqualTo(200);
            Map<String, Object> tokenJson = parseJson(token.body());
            String accessToken = (String) tokenJson.get("access_token");
            String idToken = (String) tokenJson.get("id_token");
            assertThat(accessToken).isNotBlank();
            assertThat(idToken).isNotBlank();

            SignedJWT accessJwt = SignedJWT.parse(accessToken);
            assertThat(accessJwt.getJWTClaimsSet().getIssuer()).isEqualTo(issuer);
            assertThat(accessJwt.getJWTClaimsSet().getAudience()).contains(clientId);
            assertThat((List<String>) accessJwt.getJWTClaimsSet().getClaim("userroles")).contains("jeap_@system_#read");
            assertThat(accessJwt.getJWTClaimsSet().getStringClaim("custom_access")).isEqualTo("a1");

            SignedJWT idJwt = SignedJWT.parse(idToken);
            assertThat(idJwt.getJWTClaimsSet().getStringClaim("nonce")).isEqualTo(nonce);
            assertThat(idJwt.getJWTClaimsSet().getStringClaim("custom_id")).isEqualTo("i1");

            HttpResponse<String> userInfo = httpClient.send(
                    HttpRequest.newBuilder(URI.create(issuer + "/oauth2/userinfo")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(userInfo.statusCode()).isEqualTo(200);
            Map<String, Object> userInfoJson = parseJson(userInfo.body());
            assertThat(userInfoJson)
                    .containsEntry("sub", "test-user")
                    .containsEntry("preferred_username", "test-user")
                    .containsEntry("department", "IT");
        } finally {
            server.stop();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void supportsRoleProfilesWithResettableTestBaseline() throws IOException, InterruptedException, ParseException {
        int port = freePort();
        String basePath = "/mock-idp";
        String issuer = "http://localhost:" + port + basePath;
        String redirectUri = "http://localhost/callback";

        OidcAuthorizationMockServer server = OidcAuthorizationMockServer.builder(port, basePath, "http://localhost:8080")
                .withDefaultClientId("test-client")
                .withUserRoles(List.of("jeap_@system_#read"))
                .withRoleProfile("admin", List.of("jeap_@system_#admin"))
                .build();

        HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        try {
            server.start();

            String defaultCodeVerifier = "default-code-verifier";
            String defaultCode = requestAuthorizationCode(httpClient, issuer, redirectUri, defaultCodeVerifier);
            String defaultTokenResponse = exchangeToken(httpClient, issuer, "test-client", defaultCode, defaultCodeVerifier);
            SignedJWT defaultAccessJwt = SignedJWT.parse((String) parseJson(defaultTokenResponse).get("access_token"));
            assertThat((List<String>) defaultAccessJwt.getJWTClaimsSet().getClaim("userroles"))
                    .containsExactly("jeap_@system_#read");

            server.setActiveProfile("admin");
            String activeCodeVerifier = "active-code-verifier";
            String activeCode = requestAuthorizationCode(httpClient, issuer, redirectUri, activeCodeVerifier);
            String activeTokenResponse = exchangeToken(httpClient, issuer, "test-client", activeCode, activeCodeVerifier);
            SignedJWT activeAccessJwt = SignedJWT.parse((String) parseJson(activeTokenResponse).get("access_token"));
            assertThat((List<String>) activeAccessJwt.getJWTClaimsSet().getClaim("userroles"))
                    .containsExactly("jeap_@system_#admin");

            server.reset();
            String afterResetCodeVerifier = "after-reset-code-verifier";
            String afterResetCode = requestAuthorizationCode(httpClient, issuer, redirectUri, afterResetCodeVerifier);
            String afterResetTokenResponse = exchangeToken(httpClient, issuer, "test-client", afterResetCode, afterResetCodeVerifier);
            SignedJWT afterResetAccessJwt = SignedJWT.parse((String) parseJson(afterResetTokenResponse).get("access_token"));
            assertThat((List<String>) afterResetAccessJwt.getJWTClaimsSet().getClaim("userroles"))
                    .containsExactly("jeap_@system_#read");
        } finally {
            server.stop();
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static String s256(String verifier) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String extractQueryParam(String url, String key) {
        String rawQuery = URI.create(url).getRawQuery();
        for (String pair : rawQuery.split("&")) {
            int separator = pair.indexOf('=');
            if (separator > 0 && pair.substring(0, separator).equals(key)) {
                return java.net.URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8);
            }
        }
        throw new IllegalStateException("Missing query parameter: " + key);
    }

    private static String requestAuthorizationCode(HttpClient httpClient, String issuer, String redirectUri, String codeVerifier)
            throws IOException, InterruptedException {
        String codeChallenge = s256(codeVerifier);
        String authorizeUrl = issuer + "/oauth2/authorize"
                + "?redirect_uri=" + enc(redirectUri)
                + "&state=" + enc("state-" + codeVerifier)
                + "&nonce=" + enc("nonce-" + codeVerifier)
                + "&code_challenge=" + enc(codeChallenge)
                + "&code_challenge_method=S256";

        HttpResponse<String> authorize = httpClient.send(
                HttpRequest.newBuilder(URI.create(authorizeUrl)).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(authorize.statusCode()).isEqualTo(302);
        String location = authorize.headers().firstValue("Location").orElseThrow();
        return extractQueryParam(location, "code");
    }

    private static String exchangeToken(HttpClient httpClient, String issuer, String clientId, String code, String codeVerifier)
            throws IOException, InterruptedException {
        String form = "grant_type=authorization_code"
                + "&code=" + enc(code)
                + "&code_verifier=" + enc(codeVerifier)
                + "&client_id=" + enc(clientId);

        HttpResponse<String> token = httpClient.send(
                HttpRequest.newBuilder(URI.create(issuer + "/oauth2/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(token.statusCode()).isEqualTo(200);
        return token.body();
    }

    private static Map<String, Object> parseJson(String json) throws ParseException {
        return JSONObjectUtils.parse(json);
    }
}
