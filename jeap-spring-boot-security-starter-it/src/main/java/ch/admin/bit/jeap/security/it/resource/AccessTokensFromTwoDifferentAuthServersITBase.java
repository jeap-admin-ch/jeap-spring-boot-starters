package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.it.mockserver.OAuth2MockServer;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.test.jws.JwsBuilder;
import ch.admin.bit.jeap.security.test.resource.configuration.DisableJeapPermitAllSecurityConfiguration;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;

@ActiveProfiles("resource")
@Import({DisableJeapPermitAllSecurityConfiguration.class})
public class AccessTokensFromTwoDifferentAuthServersITBase {

    private static final String SEMANTIC_AUTH_READ_ROLE = SemanticApplicationRole.builder()
            .system("jme")
            .resource("auth")
            .operation("read")
            .build().toString();

    private final OAuth2MockServer oauth2MockServer1 = new OAuth2MockServer(8098, "/auth");
    private final OAuth2MockServer oauth2MockServer2 = new OAuth2MockServer(8099, "/auth");

    private final RequestSpecification authPathSpec;

    private static String createBearerTokenForMockServer(OAuth2MockServer mockServer) {
        return JwsBuilder.createValidForFixedLongPeriod("test-subject", JeapAuthenticationContext.USER)
                .withRsaKey(mockServer.getKey())
                .withIssuer(mockServer.getIssuer())
                .withUserRoles(SEMANTIC_AUTH_READ_ROLE)
                .build().serialize();
    }

    protected  String createBearerTokenForMockServer1() {
        return createBearerTokenForMockServer(oauth2MockServer1);
    }

    protected  String createBearerTokenForMockServer2() {
        return createBearerTokenForMockServer(oauth2MockServer2);
    }

    @BeforeEach
    void setUp() {
        oauth2MockServer1.start();
        oauth2MockServer2.start();
    }

    @AfterEach
    void tearDown() {
        oauth2MockServer1.stop();
        oauth2MockServer2.stop();
    }

    protected AccessTokensFromTwoDifferentAuthServersITBase(int serverPort, String context) {
        this.authPathSpec = new RequestSpecBuilder().setBasePath(context + "/api/semantic/auth").setPort(serverPort).build();
    }

    // @formatter:off
    protected void assertHttpStatusWithTokenOnGet(String token, HttpStatus httpStatus) {
        given().
                spec(authPathSpec).
                auth().oauth2(token).
        when().
                get().
        then().
                statusCode(httpStatus.value());
    }

}
