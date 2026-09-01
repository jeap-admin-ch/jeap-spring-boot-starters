package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Audience validation tests for the strict audience validation mode 'off' (the default): a token without an audience
 * is considered valid for every resource and is accepted silently. The mode-independent tests are inherited from
 * {@link AbstractAudienceAccessTokenIT}. Stack-specific subclasses must provide an application context configured
 * with this mode.
 */
@ExtendWith(OutputCaptureExtension.class)
public abstract class AbstractStrictAudienceValidationOffIT extends AbstractAudienceAccessTokenIT {

    protected AbstractStrictAudienceValidationOffIT(int serverPort, String context) {
        super(serverPort, context);
    }

    @ParameterizedTest
    @EnumSource(value = JeapAuthenticationContext.class, names = {"USER", "SYS"})
    protected void testGetAuth_whenTokenWithoutAudience_thenAccessGrantedWithoutWarning(JeapAuthenticationContext context, CapturedOutput output) {
        assertAccessGranted(createTokenWithoutAudience(context));

        assertThat(output.getOut()).doesNotContain(STRICT_AUDIENCE_VALIDATION_LOG_MARKER);
    }

    @ParameterizedTest
    @EnumSource(value = JeapAuthenticationContext.class, names = {"USER", "SYS"})
    protected void testGetAuth_whenTokenWithEmptyAudience_thenAccessGrantedWithoutWarning(JeapAuthenticationContext context, CapturedOutput output) {
        assertAccessGranted(createTokenWithEmptyAudience(context));

        assertThat(output.getOut()).doesNotContain(STRICT_AUDIENCE_VALIDATION_LOG_MARKER);
    }

}
