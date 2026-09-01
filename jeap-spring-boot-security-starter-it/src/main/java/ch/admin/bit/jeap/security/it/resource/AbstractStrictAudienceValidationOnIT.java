package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Audience validation tests for the strict audience validation mode 'on': a token without an audience does not
 * address this resource and is rejected. The mode-independent tests are inherited from
 * {@link AbstractAudienceAccessTokenIT}. Stack-specific subclasses must provide an application context configured
 * with this mode.
 */
public abstract class AbstractStrictAudienceValidationOnIT extends AbstractAudienceAccessTokenIT {

    protected AbstractStrictAudienceValidationOnIT(int serverPort, String context) {
        super(serverPort, context);
    }

    @ParameterizedTest
    @EnumSource(value = JeapAuthenticationContext.class, names = {"USER", "SYS"})
    protected void testGetAuth_whenTokenWithoutAudience_thenAccessDenied(JeapAuthenticationContext context) {
        assertAccessDenied(createTokenWithoutAudience(context));
    }

    @ParameterizedTest
    @EnumSource(value = JeapAuthenticationContext.class, names = {"USER", "SYS"})
    protected void testGetAuth_whenTokenWithEmptyAudience_thenAccessDenied(JeapAuthenticationContext context) {
        assertAccessDenied(createTokenWithEmptyAudience(context));
    }

}
