package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.it.mockserver.OAuth2MockServer;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.test.jws.JwsBuilder;
import ch.admin.bit.jeap.security.test.resource.configuration.DisableJeapPermitAllSecurityConfiguration;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@ActiveProfiles({"resource","resource-introspection"})
@Import({DisableJeapPermitAllSecurityConfiguration.class})
public class TokenIntrospectionITBase {

    protected static final String USER_ROLES_CLAIM_NAME = "userroles";
    protected static final String BP_ROLES_CLAIM_NAME = "bproles";

    protected static final String SUBJECT = "69368608-D736-43C8-5F76-55B7BF168299";
    protected static final String EXT_ID = "342809732";
    protected static final String ADMIN_DIR_UID = "U11111111";
    protected static final String NAME = "Max Muster";
    protected static final String FAMILY_NAME = "Muster";
    protected static final String GIVEN_NAME = "Max";
    protected static final String PREFERRED_USER_NAME = "Maximilian";
    protected static final String LOCALE = "de";

    protected static final String SEMANTIC_AUTH_READ_ROLE = SemanticApplicationRole.builder()
            .system("jme")
            .resource("auth")
            .operation("read")
            .build().toString();

    protected static final String SEMANTIC_OTHER_ROLE = SemanticApplicationRole.builder()
            .system("jme")
            .resource("other")
            .operation("read")
            .build().toString();

    private static SignedJWT createBearerTokenForMockServer(OAuth2MockServer mockServer, Map<String, Object> additionalClaims) {
        JwsBuilder jwsBuilder = JwsBuilder.createValidForFixedLongPeriod(SUBJECT, JeapAuthenticationContext.USER)
                .withRsaKey(mockServer.getKey())
                .withIssuer(mockServer.getIssuer())
                .withExtId(EXT_ID)
                .withAdminDirUID(ADMIN_DIR_UID)
                .withLocale(LOCALE)
                .withFamilyName(FAMILY_NAME)
                .withGivenName(GIVEN_NAME)
                .withName(NAME)
                .withPreferredUsername(PREFERRED_USER_NAME);
        if (additionalClaims != null) {
            additionalClaims.forEach(jwsBuilder::withClaim);
        }
        return jwsBuilder.build();
    }

    private static final OAuth2MockServer oauth2MockServer = new OAuth2MockServer(0, "/auth");

    private final RequestSpecification authPathSpec;

    protected TokenIntrospectionITBase(int serverPort, String context) {
        this.authPathSpec = new RequestSpecBuilder().setBasePath(context + "/api/semantic/auth").setPort(serverPort).build();
    }

    protected SignedJWT createBearerToken(Map<String, Object> additionalClaims) {
        return createBearerTokenForMockServer(oauth2MockServer, additionalClaims);
    }

    protected void stubTokenIntrospectionRequest(JWT token, boolean tokenActive, Map<String, Object> additionalClaims) {
        oauth2MockServer.stubTokenIntrospectionRequest(token, tokenActive, additionalClaims);
    }

    protected void stubTokenIntrospectionErrorResponseRequest() {
        oauth2MockServer.stubTokenIntrospectionErrorResponseRequest();
    }

    protected void stubTokenIntrospectionRequestWithDelayedResponse(JWT token, int delayInMillis) {
        oauth2MockServer.stubTokenIntrospectionRequestWithDelayedResponse(token, delayInMillis);
    }

    protected void verifyTokenIntrospectionRequest(int times) {
        oauth2MockServer.verifyTokenIntrospectionRequest(times);
    }

    // @formatter:off
    protected ValidatableResponse assertHttpStatusAndUserInfoWithTokenOnGet(String token, HttpStatus httpStatus) {
        ValidatableResponse validatableResponse =  given().
                spec(authPathSpec).
                auth().oauth2(token).
            when().
                get().
            then();
        if (httpStatus == HttpStatus.OK) {
            return validatableResponse.
                statusCode(httpStatus.value()).
                body("subject", equalTo(SUBJECT)).
                body("extId", equalTo(EXT_ID)).
                body("adminDirUID", equalTo(ADMIN_DIR_UID)).
                body("locale", equalTo(LOCALE)).
                body("ctx", equalTo(JeapAuthenticationContext.USER.name())).
                body("name", equalTo(NAME)).
                body("givenName", equalTo(GIVEN_NAME)).
                body("familyName", equalTo(FAMILY_NAME)).
                body("preferredUsername", equalTo(PREFERRED_USER_NAME));
            }
        else {
            return validatableResponse.statusCode(httpStatus.value());
        }
    }
    // @formatter:on

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("jeap.security.oauth2.resourceserver.authorization-server.issuer", oauth2MockServer::getIssuer);
        registry.add("jeap.security.oauth2.resourceserver.authorization-server.introspection.uri", oauth2MockServer::getIntrospectionUri);
    }

    @BeforeAll
    static void init() {
        oauth2MockServer.start();
    }

    @AfterEach
    void reset() {
        oauth2MockServer.reset();
    }

    @AfterAll
    static void tearDown() {
        oauth2MockServer.stop();
    }

}
