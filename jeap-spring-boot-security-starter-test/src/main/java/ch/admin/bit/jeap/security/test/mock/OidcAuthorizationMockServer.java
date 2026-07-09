package ch.admin.bit.jeap.security.test.mock;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.test.jws.JwsBuilder;
import ch.admin.bit.jeap.security.test.jws.RSAKeyUtils;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.SignedJWT;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * Reusable OIDC Authorization Code mock server backed by WireMock.
 *
 * <p>This test utility simulates a minimal OpenID Connect provider with:
 * well-known discovery, JWKS, authorize, token and userinfo endpoints.
 * It supports Authorization Code + PKCE and configurable identity/role claims,
 * so it can be reused in integration and end-to-end tests across projects.</p>
 */
public class OidcAuthorizationMockServer {

    private static final List<String> DEFAULT_USER_ROLES = List.of("jme_@person_#read", "jme_@person_#write");
    private static final String DEFAULT_PROFILE_NAME = "default";
    private static final String DEFAULT_CLIENT_ID = "jme-jeap-nivel-quadrel-project-template";
    private static final String DEFAULT_SUBJECT = "mock-user";
    private static final String DEFAULT_PREFERRED_USERNAME = "mock-user";
    private static final String DEFAULT_EMAIL = "mock-user@example.com";
    private static final String DEFAULT_SCOPE = "openid profile roles";
    private static final String PATH_DELIMITER = "/";
    private static final String ERROR_KEY = "error";

    private final String allowedOrigin;
    private final WireMockServer server;
    private final RSAKey privateKey;
    private final RSAKey publicKey;
    private final Map<String, OAuthMockProfile> profiles;

    private final String issuer;
    private final String configPath;
    private final String jwksPath;
    private final String authorizePath;
    private final String tokenPath;
    private final String userInfoPath;
    private final String defaultProfileName;

    private final Map<String, AuthCodeRecord> authCodes = new ConcurrentHashMap<>();
    private final Map<String, OAuthMockProfile> profilesByAccessToken = new ConcurrentHashMap<>();
    private volatile String activeProfileName;

    public OidcAuthorizationMockServer(int port, String basePath, String allowedOrigin) {
        this(builder(port, basePath, allowedOrigin));
    }

    private OidcAuthorizationMockServer(Builder builder) {
        String normalizedBasePath = builder.basePath.startsWith(PATH_DELIMITER) ? builder.basePath : PATH_DELIMITER + builder.basePath;
        this.allowedOrigin = builder.allowedOrigin;
        this.issuer = "http://localhost:" + builder.port + normalizedBasePath;
        this.profiles = builder.profiles();
        this.defaultProfileName = builder.defaultProfileName;
        this.activeProfileName = defaultProfileName;

        this.configPath = normalizedBasePath + "/.well-known/openid-configuration";
        this.jwksPath = normalizedBasePath + "/.well-known/jwks.json";
        this.authorizePath = normalizedBasePath + "/oauth2/authorize";
        this.tokenPath = normalizedBasePath + "/oauth2/token";
        this.userInfoPath = normalizedBasePath + "/oauth2/userinfo";

        RSAKey keyPair = RSAKeyUtils.createRsaKeyPair();
        this.privateKey = keyPair;
        this.publicKey = keyPair.toPublicJWK();

        OAuthDynamicTransformer transformer = new OAuthDynamicTransformer();
        this.server = new WireMockServer(WireMockConfiguration.options()
                .bindAddress("localhost")
                .port(builder.port)
                .extensions(transformer));
        registerRoutes();
    }

    public static Builder builder(int port, String basePath, String allowedOrigin) {
        return new Builder(port, basePath, allowedOrigin);
    }

    /**
     * Starts the mock HTTP server.
     */
    public void start() {
        server.start();
    }

    /**
     * Stops the mock HTTP server.
     */
    public void stop() {
        server.stop();
    }

    /**
     * Clears runtime state (issued authorization codes and request history).
     */
    public void reset() {
        authCodes.clear();
        profilesByAccessToken.clear();
        activeProfileName = defaultProfileName;
        server.resetRequests();
    }

    /**
     * Sets the active profile used for subsequent authorize requests.
     */
    public void setActiveProfile(String profileName) {
        this.activeProfileName = resolveProfile(profileName).name();
    }

    private void registerRoutes() {
        server.stubFor(any(urlPathEqualTo(configPath))
                .willReturn(ResponseDefinitionBuilder.responseDefinition().withTransformers(OAuthDynamicTransformer.NAME)));
        server.stubFor(any(urlPathEqualTo(jwksPath))
                .willReturn(ResponseDefinitionBuilder.responseDefinition().withTransformers(OAuthDynamicTransformer.NAME)));
        server.stubFor(any(urlPathEqualTo(authorizePath))
                .willReturn(ResponseDefinitionBuilder.responseDefinition().withTransformers(OAuthDynamicTransformer.NAME)));
        server.stubFor(any(urlPathEqualTo(tokenPath))
                .willReturn(ResponseDefinitionBuilder.responseDefinition().withTransformers(OAuthDynamicTransformer.NAME)));
        server.stubFor(any(urlPathEqualTo(userInfoPath))
                .willReturn(ResponseDefinitionBuilder.responseDefinition().withTransformers(OAuthDynamicTransformer.NAME)));
    }

    private SignedJWT createToken(Duration tokenExpiresIn, String clientId, String nonce, OAuthMockProfile profile) {
        ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        ZonedDateTime expiry = now.plus(tokenExpiresIn);

        JwsBuilder builder = JwsBuilder.create(
                        UUID.randomUUID().toString(),
                        issuer,
                        expiry,
                        now,
                        now,
                        profile.subject(),
                        JeapAuthenticationContext.USER)
                .withRsaKey(privateKey)
                .withAudiences(clientId)
                .withUserRoles(profile.userRoles().toArray(String[]::new))
                .withClaim("roles", profile.userRoles());
        if (nonce != null && !nonce.isBlank()) {
            builder.withClaim("nonce", nonce);
            profile.idTokenClaims().forEach(builder::withClaim);
        } else {
            profile.accessTokenClaims().forEach(builder::withClaim);
        }
        return builder.build();
    }

    public static class Builder {
        private final int port;
        private final String basePath;
        private final String allowedOrigin;

        private String defaultClientId = DEFAULT_CLIENT_ID;
        private String subject = DEFAULT_SUBJECT;
        private String preferredUsername = DEFAULT_PREFERRED_USERNAME;
        private String email = DEFAULT_EMAIL;
        private String scope = DEFAULT_SCOPE;
        private List<String> userRoles = DEFAULT_USER_ROLES;
        private String defaultProfileName = DEFAULT_PROFILE_NAME;
        private final Map<String, List<String>> roleProfiles = new HashMap<>();
        private final Map<String, Object> accessTokenClaims = new HashMap<>();
        private final Map<String, Object> idTokenClaims = new HashMap<>();
        private final Map<String, Object> userInfoClaims = new HashMap<>();

        private Builder(int port, String basePath, String allowedOrigin) {
            this.port = port;
            this.basePath = Objects.requireNonNull(basePath, "basePath must not be null");
            this.allowedOrigin = Objects.requireNonNull(allowedOrigin, "allowedOrigin must not be null");
        }

        /**
         * Overrides the client id used when no {@code client_id} is sent to the token endpoint.
         */
        public Builder withDefaultClientId(String defaultClientId) {
            this.defaultClientId = requireNotBlank(defaultClientId, "defaultClientId");
            return this;
        }

        /**
         * Sets the {@code sub} claim for ID token and userinfo responses.
         */
        public Builder withSubject(String subject) {
            this.subject = requireNotBlank(subject, "subject");
            return this;
        }

        /**
         * Sets the {@code preferred_username} claim for userinfo responses.
         */
        public Builder withPreferredUsername(String preferredUsername) {
            this.preferredUsername = requireNotBlank(preferredUsername, "preferredUsername");
            return this;
        }

        /**
         * Sets the {@code email} claim for userinfo responses.
         */
        public Builder withEmail(String email) {
            this.email = requireNotBlank(email, "email");
            return this;
        }

        /**
         * Sets the returned scope string in token responses.
         */
        public Builder withScope(String scope) {
            this.scope = requireNotBlank(scope, "scope");
            return this;
        }

        /**
         * Sets role claims used in tokens and userinfo.
         */
        public Builder withUserRoles(List<String> userRoles) {
            if (userRoles == null || userRoles.isEmpty()) {
                throw new IllegalArgumentException("userRoles must not be null or empty");
            }
            this.userRoles = List.copyOf(userRoles);
            return this;
        }

        /**
         * Adds a named role profile. The profile inherits all non-role settings from the builder defaults.
         */
        public Builder withRoleProfile(String profileName, List<String> userRoles) {
            String normalizedProfileName = requireNotBlank(profileName, "profileName");
            if (userRoles == null || userRoles.isEmpty()) {
                throw new IllegalArgumentException("userRoles must not be null or empty");
            }
            roleProfiles.put(normalizedProfileName, List.copyOf(userRoles));
            return this;
        }

        /**
         * Sets which profile name is active when the server starts and after {@link OidcAuthorizationMockServer#reset()}.
         */
        public Builder withDefaultProfile(String profileName) {
            this.defaultProfileName = requireNotBlank(profileName, "profileName");
            return this;
        }

        /**
         * Adds custom claims to access tokens.
         */
        public Builder withAccessTokenClaims(Map<String, Object> claims) {
            if (claims != null) {
                this.accessTokenClaims.putAll(claims);
            }
            return this;
        }

        /**
         * Adds custom claims to ID tokens.
         */
        public Builder withIdTokenClaims(Map<String, Object> claims) {
            if (claims != null) {
                this.idTokenClaims.putAll(claims);
            }
            return this;
        }

        /**
         * Adds custom claims to userinfo responses.
         */
        public Builder withUserInfoClaims(Map<String, Object> claims) {
            if (claims != null) {
                this.userInfoClaims.putAll(claims);
            }
            return this;
        }

        /**
         * Builds a new configured mock server instance.
         */
        public OidcAuthorizationMockServer build() {
            return new OidcAuthorizationMockServer(this);
        }

        private Map<String, OAuthMockProfile> profiles() {
            OAuthMockProfile defaultProfile = new OAuthMockProfile(
                    DEFAULT_PROFILE_NAME,
                    defaultClientId,
                    subject,
                    preferredUsername,
                    email,
                    scope,
                    List.copyOf(userRoles),
                    Map.copyOf(accessTokenClaims),
                    Map.copyOf(idTokenClaims),
                    Map.copyOf(userInfoClaims));
            Map<String, OAuthMockProfile> configuredProfiles = new HashMap<>();
            configuredProfiles.put(defaultProfile.name(), defaultProfile);
            roleProfiles.forEach((profileName, roles) -> configuredProfiles.put(
                    profileName, defaultProfile.withNameAndRoles(profileName, roles)));
            if (!configuredProfiles.containsKey(defaultProfileName)) {
                throw new IllegalArgumentException("Unknown default profile: " + defaultProfileName);
            }
            return Map.copyOf(configuredProfiles);
        }

        private static String requireNotBlank(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " must not be null or blank");
            }
            return value;
        }
    }

    private record OAuthMockProfile(
            String name,
            String defaultClientId,
            String subject,
            String preferredUsername,
            String email,
            String scope,
            List<String> userRoles,
            Map<String, Object> accessTokenClaims,
            Map<String, Object> idTokenClaims,
            Map<String, Object> userInfoClaims
    ) {
        OAuthMockProfile withNameAndRoles(String profileName, List<String> roles) {
            return new OAuthMockProfile(
                    profileName,
                    defaultClientId,
                    subject,
                    preferredUsername,
                    email,
                    scope,
                    List.copyOf(roles),
                    accessTokenClaims,
                    idTokenClaims,
                    userInfoClaims
            );
        }
    }

    private record AuthCodeRecord(String codeChallenge, String nonce, String profileName, long expiresAt) {
        boolean expired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private class OAuthDynamicTransformer implements ResponseTransformerV2 {

        static final String NAME = "oauth-dynamic-transformer";

        @Override
        public Response transform(Response response, ServeEvent serveEvent) {
            Request request = serveEvent.getRequest();
            String path = request.getUrl();
            int queryStart = path.indexOf('?');
            if (queryStart >= 0) {
                path = path.substring(0, queryStart);
            }

            if ("OPTIONS".equalsIgnoreCase(request.getMethod().value())) {
                return response(204, null);
            }

            if (configPath.equals(path)) {
                return handleWellKnownConfiguration(request);
            }
            if (jwksPath.equals(path)) {
                return handleJwks(request);
            }
            if (authorizePath.equals(path)) {
                return handleAuthorize(request);
            }
            if (tokenPath.equals(path)) {
                return handleToken(request);
            }
            if (userInfoPath.equals(path)) {
                return handleUserInfo(request);
            }
            return response(404, null);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public boolean applyGlobally() {
            return false;
        }

        private Response handleWellKnownConfiguration(Request request) {
            if (!"GET".equalsIgnoreCase(request.getMethod().value())) {
                return response(405, null);
            }
            Map<String, Object> body = Map.of(
                    "issuer", issuer,
                    "authorization_endpoint", issuer + "/oauth2/authorize",
                    "token_endpoint", issuer + "/oauth2/token",
                    "userinfo_endpoint", issuer + "/oauth2/userinfo",
                    "jwks_uri", issuer + "/.well-known/jwks.json",
                    "subject_types_supported", List.of("public"),
                    "response_types_supported", List.of("code"),
                    "grant_types_supported", List.of("authorization_code"),
                    "code_challenge_methods_supported", List.of("S256")
            );
            return json(200, body);
        }

        private Response handleJwks(Request request) {
            if (!"GET".equalsIgnoreCase(request.getMethod().value())) {
                return response(405, null);
            }
            return json(200, new JWKSet(publicKey).toJSONObject());
        }

        private Response handleAuthorize(Request request) {
            if (!"GET".equalsIgnoreCase(request.getMethod().value())) {
                return response(405, null);
            }
            String redirectUri = queryParameter(request, "redirect_uri");
            if (redirectUri == null || redirectUri.isBlank()) {
                return json(400, Map.of(ERROR_KEY, "invalid_request"));
            }
            String state = queryParameter(request, "state");
            String codeChallenge = queryParameter(request, "code_challenge");
            String nonce = queryParameter(request, "nonce");
            OAuthMockProfile requestedProfile = resolveProfile(activeProfileName);

            String code = UUID.randomUUID().toString();
            authCodes.put(code, new AuthCodeRecord(
                    codeChallenge,
                    nonce,
                    requestedProfile.name(),
                    System.currentTimeMillis() + Duration.ofMinutes(10).toMillis()));

            String location = redirectUri
                    + (redirectUri.contains("?") ? "&" : "?")
                    + "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + ((state == null || state.isEmpty()) ? "" : "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8));

            return response(302, null, Map.of("Location", location));
        }

        private Response handleToken(Request request) {
            if (!"POST".equalsIgnoreCase(request.getMethod().value())) {
                return response(405, null);
            }

            Map<String, String> form = new HashMap<>();
            request.formParameters().forEach((key, parameter) -> form.put(key, parameter.firstValue()));
            if (!"authorization_code".equals(form.get("grant_type"))) {
                return json(400, Map.of(ERROR_KEY, "unsupported_grant_type"));
            }

            String code = form.get("code");
            String codeVerifier = form.get("code_verifier");

            AuthCodeRecord authCodeRecord = authCodes.remove(code);
            if (authCodeRecord == null || authCodeRecord.expired()) {
                return json(400, Map.of(ERROR_KEY, "invalid_grant"));
            }
            OAuthMockProfile tokenProfile = resolveProfile(authCodeRecord.profileName());
            if (authCodeRecord.codeChallenge != null && !authCodeRecord.codeChallenge.isBlank()
                    && (codeVerifier == null || !authCodeRecord.codeChallenge.equals(s256Challenge(codeVerifier)))) {
                return json(400, Map.of(ERROR_KEY, "invalid_grant"));
            }
            String clientId = form.getOrDefault("client_id", tokenProfile.defaultClientId());

            SignedJWT accessToken = createToken(Duration.ofHours(1), clientId, null, tokenProfile);
            SignedJWT idToken = createToken(Duration.ofHours(1), clientId, authCodeRecord.nonce(), tokenProfile);

            long expiresIn;
            try {
                expiresIn = Math.max((accessToken.getJWTClaimsSet().getExpirationTime().getTime() - System.currentTimeMillis()) / 1000, 0);
            } catch (ParseException e) {
                return json(500, Map.of(ERROR_KEY, "server_error"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("access_token", accessToken.serialize());
            response.put("id_token", idToken.serialize());
            response.put("token_type", "bearer");
            response.put("expires_in", expiresIn);
            response.put("scope", tokenProfile.scope());
            profilesByAccessToken.put(accessToken.serialize(), tokenProfile);
            return json(200, response);
        }

        private Response handleUserInfo(Request request) {
            if (!"GET".equalsIgnoreCase(request.getMethod().value())) {
                return response(405, null);
            }
            OAuthMockProfile resolvedProfile = resolveProfileFromUserInfoRequest(request);
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("sub", resolvedProfile.subject());
            userInfo.put("name", "Mock User");
            userInfo.put("preferred_username", resolvedProfile.preferredUsername());
            userInfo.put("email", resolvedProfile.email());
            userInfo.put("userroles", resolvedProfile.userRoles());
            userInfo.put("bproles", Map.of("default", resolvedProfile.userRoles()));
            userInfo.putAll(resolvedProfile.userInfoClaims());
            return json(200, userInfo);
        }

        private OAuthMockProfile resolveProfileFromUserInfoRequest(Request request) {
            String authorization = request.getHeader("Authorization");
            if (authorization == null || authorization.isBlank() || !authorization.startsWith("Bearer ")) {
                return resolveProfile(activeProfileName);
            }
            String accessToken = authorization.substring("Bearer ".length()).trim();
            if (accessToken.isEmpty()) {
                return resolveProfile(activeProfileName);
            }
            return profilesByAccessToken.getOrDefault(accessToken, resolveProfile(activeProfileName));
        }

        private Response json(int status, Map<String, ?> body) {
            return response(status, JSONObjectUtils.toJSONString(body), Map.of("Content-Type", "application/json"));
        }

        private Response response(int status, String body) {
            return response(status, body, Map.of());
        }

        private Response response(int status, String body, Map<String, String> headers) {
            Response.Builder responseBuilder = Response.response()
                    .status(status)
                    .headers(withCorsHeaders(headers));
            if (body != null) {
                responseBuilder.body(body);
            }
            return responseBuilder.build();
        }

        private HttpHeaders withCorsHeaders(Map<String, String> headers) {
            List<HttpHeader> allHeaders = new ArrayList<>();
            headers.forEach((name, value) -> allHeaders.add(HttpHeader.httpHeader(name, value)));
            allHeaders.add(HttpHeader.httpHeader("Access-Control-Allow-Origin", allowedOrigin));
            allHeaders.add(HttpHeader.httpHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS"));
            allHeaders.add(HttpHeader.httpHeader("Access-Control-Allow-Headers", "Authorization,Content-Type"));
            return new HttpHeaders(allHeaders);
        }

        private String s256Challenge(String verifier) {
            try {
                return Base64.getUrlEncoder().withoutPadding().encodeToString(
                        MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Failed to compute PKCE challenge", e);
            }
        }

        private String queryParameter(Request request, String name) {
            if (!request.queryParameter(name).isPresent()) {
                return null;
            }
            return request.queryParameter(name).firstValue();
        }
    }

    private OAuthMockProfile resolveProfile(String profileName) {
        OAuthMockProfile resolvedProfile = profiles.get(profileName);
        if (resolvedProfile == null) {
            throw new IllegalArgumentException("Unknown profile: " + profileName);
        }
        return resolvedProfile;
    }
}
