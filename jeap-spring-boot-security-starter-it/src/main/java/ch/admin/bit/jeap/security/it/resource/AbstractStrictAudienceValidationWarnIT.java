package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Audience validation tests for the strict audience validation mode 'warn': a token without an audience is accepted,
 * but a warning is logged, as the token would be rejected in mode 'on'. The mode-independent tests are inherited from
 * {@link AbstractAudienceAccessTokenIT}. Stack-specific subclasses must provide an application context configured
 * with this mode.
 */
@ExtendWith(OutputCaptureExtension.class)
public abstract class AbstractStrictAudienceValidationWarnIT extends AbstractAudienceAccessTokenIT {

    private static final String TOKEN_WITHOUT_AUDIENCE_ACCEPTED_WARNING = "Token without audience accepted (strict-audience-validation=warn)";

    protected AbstractStrictAudienceValidationWarnIT(int serverPort, String context) {
        super(serverPort, context);
    }

    @ParameterizedTest
    @EnumSource(value = JeapAuthenticationContext.class, names = {"USER", "SYS"})
    protected void testGetAuth_whenTokenWithoutAudience_thenAccessGrantedWithWarning(JeapAuthenticationContext context, CapturedOutput output) {
        assertAccessGranted(createTokenWithoutAudience(context));

        assertThat(output.getOut()).contains(TOKEN_WITHOUT_AUDIENCE_ACCEPTED_WARNING);
    }

    @ParameterizedTest
    @EnumSource(value = JeapAuthenticationContext.class, names = {"USER", "SYS"})
    protected void testGetAuth_whenTokenWithEmptyAudience_thenAccessGrantedWithWarning(JeapAuthenticationContext context, CapturedOutput output) {
        assertAccessGranted(createTokenWithEmptyAudience(context));

        assertThat(output.getOut()).contains(TOKEN_WITHOUT_AUDIENCE_ACCEPTED_WARNING);
    }

}
