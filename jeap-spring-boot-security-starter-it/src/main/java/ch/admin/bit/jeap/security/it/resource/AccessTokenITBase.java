package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.test.jws.JwsBuilder;
import ch.admin.bit.jeap.security.test.jws.JwsBuilderFactory;
import ch.admin.bit.jeap.security.test.resource.configuration.JeapOAuth2IntegrationTestResourceConfiguration;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;

@ActiveProfiles("resource")
@TestPropertySource(properties = {
        // Set the authorization serve and b2b gateway configurations to use the test support issuer and JWKS endpoint
        "jeap.security.oauth2.resourceserver.authorization-server.issuer=" + JwsBuilder.DEFAULT_ISSUER,
        "jeap.security.oauth2.resourceserver.authorization-server.jwk-set-uri=http://localhost:${server.port}/${spring.application.name}/.well-known/jwks.json",
        "jeap.security.oauth2.resourceserver.b2b-gateway.issuer=" + JwsBuilder.B2B_ISSUER,
        "jeap.security.oauth2.resourceserver.b2b-gateway.jwk-set-uri=http://localhost:${server.port}/${spring.application.name}/.well-known/jwks.json"})
@Import(JeapOAuth2IntegrationTestResourceConfiguration.class)
public class AccessTokenITBase {

    protected static final String PARTNER_ID_PARAM_NAME = "id";
    private static final String AUTH_PATH_PATTERN = "/%s/auth";
    private static final String AUTH_FOR_PARTNER_PATH_PATTERN = "/%s/{" + PARTNER_ID_PARAM_NAME + "}/auth";
    private static final String SEMANTIC_SUB_PATH = "semantic";
    private static final String SIMPLE_SUB_PATH = "simple";
    private static final String AUTHORITIES_SUB_PATH = "authorities";
    private static final String PROGRAMMATIC_PATH_SUFFIX = "-programmatic";
    private static final String SEMANTIC_PROGRAMMATIC_SUB_PATH = SEMANTIC_SUB_PATH + PROGRAMMATIC_PATH_SUFFIX;
    private static final String SIMPLE_PROGRAMMATIC_SUB_PATH = SIMPLE_SUB_PATH + PROGRAMMATIC_PATH_SUFFIX;
    private static final String AUTHORITIES_PROGRAMMATIC_SUB_PATH = AUTHORITIES_SUB_PATH + PROGRAMMATIC_PATH_SUFFIX;

    protected final String baseUrl;
    protected final RequestSpecification semanticAuthPathSpec;
    protected final RequestSpecification semanticProgrammaticAuthPathSpec;
    protected final RequestSpecification semanticAuthForPartnerPathTemplateSpec;
    protected final RequestSpecification semanticProgrammaticAuthForPartnerPathTemplateSpec;
    protected final RequestSpecification simpleAuthPathSpec;
    protected final RequestSpecification simpleProgrammaticAuthPathSpec;
    protected final RequestSpecification simpleAuthForPartnerPathTemplateSpec;
    protected final RequestSpecification simpleProgrammaticAuthForPartnerPathTemplateSpec;
    protected final RequestSpecification authoritiesAuthPathSpec;
    protected final RequestSpecification authoritiesProgrammaticAuthPathSpec;
    protected final RequestSpecification errorAuthPathSpec;

    @SuppressWarnings({"SpringJavaAutowiredMembersInspection"})
    @Autowired
    protected JwsBuilderFactory jwsBuilderFactory;

    @Value("${spring.application.name}")
    protected String applicationName;

    protected AccessTokenITBase(int serverPort, String context) {
        this.baseUrl = context + "/api";
        this.semanticAuthPathSpec = getAuthSpec(baseUrl, AUTH_PATH_PATTERN, SEMANTIC_SUB_PATH, serverPort);
        this.semanticProgrammaticAuthPathSpec = getAuthSpec(baseUrl, AUTH_PATH_PATTERN, SEMANTIC_PROGRAMMATIC_SUB_PATH, serverPort);
        this.semanticAuthForPartnerPathTemplateSpec = getAuthSpec(baseUrl, AUTH_FOR_PARTNER_PATH_PATTERN, SEMANTIC_SUB_PATH, serverPort);
        this.semanticProgrammaticAuthForPartnerPathTemplateSpec = getAuthSpec(baseUrl, AUTH_FOR_PARTNER_PATH_PATTERN, SEMANTIC_PROGRAMMATIC_SUB_PATH, serverPort);
        this.simpleAuthPathSpec = getAuthSpec(baseUrl, AUTH_PATH_PATTERN, SIMPLE_SUB_PATH, serverPort);
        this.simpleProgrammaticAuthPathSpec = getAuthSpec(baseUrl, AUTH_PATH_PATTERN, SIMPLE_PROGRAMMATIC_SUB_PATH, serverPort);
        this.simpleAuthForPartnerPathTemplateSpec = getAuthSpec(baseUrl, AUTH_FOR_PARTNER_PATH_PATTERN, SIMPLE_SUB_PATH, serverPort);
        this.simpleProgrammaticAuthForPartnerPathTemplateSpec = getAuthSpec(baseUrl, AUTH_FOR_PARTNER_PATH_PATTERN, SIMPLE_PROGRAMMATIC_SUB_PATH, serverPort);
        this.authoritiesAuthPathSpec = getAuthSpec(baseUrl, AUTH_PATH_PATTERN, AUTHORITIES_SUB_PATH, serverPort);
        this.authoritiesProgrammaticAuthPathSpec = getAuthSpec(baseUrl, AUTH_PATH_PATTERN, AUTHORITIES_PROGRAMMATIC_SUB_PATH, serverPort);
        this.errorAuthPathSpec = new RequestSpecBuilder().setBasePath(baseUrl + "/error").setPort(serverPort).build();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private static RequestSpecification getAuthSpec(String baseUrl, String pathPattern, String subPath, int port) {
        return  new RequestSpecBuilder().setBasePath(baseUrl + String.format(pathPattern, subPath)).setPort(port).build();
    }

    protected void assertHttpStatusWithTokenOnGetToAuthResource(String token, HttpStatus httpStatus) {
        assertHttpStatusWithTokenOnGet(semanticAuthPathSpec, token, httpStatus);
    }

    // @formatter:off
    protected ValidatableResponse assertHttpStatusWithTokenOnGet(RequestSpecification requestSpec, String token, HttpStatus httpStatus) {
        return given().
                spec(requestSpec).
                auth().oauth2(token).
        when().
                get().
        then().
                statusCode(httpStatus.value());
    }

    protected ValidatableResponse assertHttpStatusWithTokenOnGetForPartner(RequestSpecification requestSpec, String token, String partnerId, HttpStatus httpStatus) {
        return given().
                spec(requestSpec).
                pathParam(PARTNER_ID_PARAM_NAME, partnerId).
                auth().oauth2(token).
        when().
                get().
        then().
                statusCode(httpStatus.value());
    }

}
