package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.test.jws.JwsBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static ch.admin.bit.jeap.security.it.resource.TestRoles.SEMANTIC_AUTH_READ_ROLE;
import static ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext.B2B;
import static ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext.USER;

/**
 * Audience validation tests whose outcome does not depend on the strict audience validation mode. They are inherited
 * by the mode-specific test classes {@link AbstractStrictAudienceValidationOffIT},
 * {@link AbstractStrictAudienceValidationWarnIT} and {@link AbstractStrictAudienceValidationOnIT}, and thus run against
 * every mode, proving that the mode only affects tokens without an audience (no 'aud' claim, or an empty one). The test
 * methods are annotated here, so that stack-specific subclasses only need to provide the application context.
 * <p>
 */
@TestPropertySource(properties = "jeap.security.oauth2.resourceserver.resource-id=" + AbstractAudienceAccessTokenIT.RESOURCE_ID)
public abstract class AbstractAudienceAccessTokenIT extends AccessTokenITBase {

    /**
     * Common part of the log messages the resource server writes about tokens without an audience in the strict
     * audience validation modes 'warn' and 'on'.
     */
    protected static final String STRICT_AUDIENCE_VALIDATION_LOG_MARKER = "strict-audience-validation=";

    protected static final String RESOURCE_ID = "test-resource-id";

    private static final String SUBJECT = "69368608-D736-43C8-5F76-55B7BF168299";
    private static final String OTHER_AUDIENCE = "other";

    protected AbstractAudienceAccessTokenIT(int serverPort, String context) {
        super(serverPort, context);
    }

    @ParameterizedTest
    @EnumSource(JeapAuthenticationContext.class)
    protected void testGetAuth_whenAudienceContainsResourceId_thenAccessGranted(JeapAuthenticationContext context) {
        assertAccessGranted(createTokenWithAudience(context, RESOURCE_ID));
    }

    @ParameterizedTest
    @EnumSource(value = JeapAuthenticationContext.class, names = {"USER", "SYS"})
    protected void testGetAuth_whenAudienceDoesNotContainResourceId_thenAccessDenied(JeapAuthenticationContext context) {
        assertAccessDenied(createTokenWithAudience(context, OTHER_AUDIENCE));
    }

    @Test
    protected void testGetAuth_whenAudienceContainsApplicationNameButNotResourceId_thenAccessDenied() {
        // the explicitly configured resource id replaces the application name as the expected audience
        assertAccessDenied(createTokenWithAudience(USER, applicationName));
    }

    @Test
    protected void testGetAuth_whenB2bTokenAudienceDoesNotContainResourceId_thenAccessGranted() {
        // B2B tokens are never audience-checked as the B2B gateway cannot restrict the audience of the tokens it issues
        assertAccessGranted(createTokenWithAudience(B2B, OTHER_AUDIENCE));
    }

    @Test
    protected void testGetAuth_whenB2bTokenWithoutAudience_thenAccessGranted() {
        assertAccessGranted(createTokenWithoutAudience(B2B));
    }

    @Test
    protected void testGetAuth_whenB2bTokenWithEmptyAudience_thenAccessGranted() {
        assertAccessGranted(createTokenWithEmptyAudience(B2B));
    }

    protected String createTokenWithAudience(JeapAuthenticationContext context, String audience) {
        return createTokenBuilder(context).withAudiences(audience).build().serialize();
    }

    protected String createTokenWithoutAudience(JeapAuthenticationContext context) {
        return createTokenBuilder(context).build().serialize(); // no audiences given -> no 'aud' claim
    }

    protected String createTokenWithEmptyAudience(JeapAuthenticationContext context) {
        return createTokenBuilder(context).withEmptyAudience().build().serialize(); // -> "aud": []
    }

    protected void assertAccessGranted(String token) {
        assertHttpStatusWithTokenOnGetToAuthResource(token, HttpStatus.OK);
    }

    protected void assertAccessDenied(String token) {
        assertHttpStatusWithTokenOnGetToAuthResource(token, HttpStatus.UNAUTHORIZED);
    }

    private JwsBuilder createTokenBuilder(JeapAuthenticationContext context) {
        return jwsBuilderFactory.createValidForFixedLongPeriodBuilder(SUBJECT, context)
                .withUserRoles(SEMANTIC_AUTH_READ_ROLE); // grants access to the auth resource
    }

}
