package ch.admin.bit.jeap.security.resource.validation;

import ch.admin.bit.jeap.security.resource.properties.StrictAudienceValidationMode;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext.B2B;
import static ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext.SYS;
import static ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext.USER;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class AudienceJwtValidatorTest {

    private static final String RESOURCE_NAME = "resource-name";
    private static final String OTHER_AUDIENCE = "other-audience";
    private static final String YET_ANOTHER_AUDIENCE = "yet-another-audience";
    private static final String ISSUER = "https://issuer/auth/realm";
    private static final String SUBJECT = "test-subject";
    private static final String CLIENT_ID = "test-client";

    private static final List<String> AUDIENCE_INCLUDING_RESOURCE = List.of(OTHER_AUDIENCE, RESOURCE_NAME, YET_ANOTHER_AUDIENCE);
    private static final List<String> AUDIENCE_EXCLUDING_RESOURCE = List.of(OTHER_AUDIENCE, YET_ANOTHER_AUDIENCE);
    private static final List<String> AUDIENCE_EMPTY = List.of();
    private static final List<String> AUDIENCE_MISSING = null;

    private static final String AUDIENCE_MISMATCH_ERROR = "The token is not valid for audience '" + RESOURCE_NAME + "'.";
    private static final String AUDIENCE_MISSING_ERROR = "The token does not specify an audience and is therefore not valid for audience '" + RESOURCE_NAME + "'.";
    private static final String STRICT_VALIDATION_LOG_MARKER = "strict-audience-validation=";
    private static final String ACCEPTED_WITH_WARNING_LOG = "Token without audience accepted (strict-audience-validation=warn)";
    private static final String REJECTED_LOG = "Token without audience rejected (strict-audience-validation=on)";

    @ParameterizedTest
    @MethodSource("allModesWithAllContexts")
    void validate_whenAudienceIncludesResource_thenSuccess(StrictAudienceValidationMode mode, JeapAuthenticationContext context) {
        AudienceJwtValidator validator = new AudienceJwtValidator(RESOURCE_NAME, mode);

        OAuth2TokenValidatorResult result = validator.validate(createJwt(AUDIENCE_INCLUDING_RESOURCE, context));

        assertThat(result.getErrors()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("allModesWithAudienceCheckedContexts")
    void validate_whenAudienceExcludesResource_thenFailure(StrictAudienceValidationMode mode, JeapAuthenticationContext context) {
        AudienceJwtValidator validator = new AudienceJwtValidator(RESOURCE_NAME, mode);

        OAuth2TokenValidatorResult result = validator.validate(createJwt(AUDIENCE_EXCLUDING_RESOURCE, context));

        assertThat(result.getErrors()).extracting(OAuth2Error::getDescription).containsExactly(AUDIENCE_MISMATCH_ERROR);
    }

    @ParameterizedTest
    @EnumSource(StrictAudienceValidationMode.class)
    void validate_whenAudienceExcludesResourceInB2bContext_thenSuccess(StrictAudienceValidationMode mode) {
        AudienceJwtValidator validator = new AudienceJwtValidator(RESOURCE_NAME, mode);

        OAuth2TokenValidatorResult result = validator.validate(createJwt(AUDIENCE_EXCLUDING_RESOURCE, B2B));

        assertThat(result.getErrors()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("allModesWithMissingOrEmptyAudience")
    void validate_whenAudienceMissingInB2bContext_thenSuccessWithoutWarning(StrictAudienceValidationMode mode, List<String> audience, CapturedOutput output) {
        AudienceJwtValidator validator = new AudienceJwtValidator(RESOURCE_NAME, mode);

        OAuth2TokenValidatorResult result = validator.validate(createJwt(audience, B2B));

        assertThat(result.getErrors()).isEmpty();
        assertThat(output.getOut()).doesNotContain(STRICT_VALIDATION_LOG_MARKER);
    }

    @ParameterizedTest
    @MethodSource("audienceCheckedContextsWithMissingOrEmptyAudience")
    void validate_whenAudienceMissingAndModeOff_thenSuccessWithoutWarning(JeapAuthenticationContext context, List<String> audience, CapturedOutput output) {
        AudienceJwtValidator validator = new AudienceJwtValidator(RESOURCE_NAME, StrictAudienceValidationMode.OFF);

        OAuth2TokenValidatorResult result = validator.validate(createJwt(audience, context));

        assertThat(result.getErrors()).isEmpty();
        assertThat(output.getOut()).doesNotContain(STRICT_VALIDATION_LOG_MARKER);
    }

    @ParameterizedTest
    @MethodSource("audienceCheckedContextsWithMissingOrEmptyAudience")
    void validate_whenAudienceMissingAndModeWarn_thenSuccessWithWarningIdentifyingTheToken(JeapAuthenticationContext context, List<String> audience, CapturedOutput output) {
        AudienceJwtValidator validator = new AudienceJwtValidator(RESOURCE_NAME, StrictAudienceValidationMode.WARN);

        OAuth2TokenValidatorResult result = validator.validate(createJwt(audience, context));

        assertThat(result.getErrors()).isEmpty();
        assertThat(output.getOut())
                .contains(ACCEPTED_WITH_WARNING_LOG)
                .contains(ISSUER, SUBJECT, CLIENT_ID, context.name(), RESOURCE_NAME);
    }

    @ParameterizedTest
    @MethodSource("audienceCheckedContextsWithMissingOrEmptyAudience")
    void validate_whenAudienceMissingAndModeOn_thenFailureWithWarningIdentifyingTheToken(JeapAuthenticationContext context, List<String> audience, CapturedOutput output) {
        AudienceJwtValidator validator = new AudienceJwtValidator(RESOURCE_NAME, StrictAudienceValidationMode.ON);

        OAuth2TokenValidatorResult result = validator.validate(createJwt(audience, context));

        assertThat(result.getErrors()).extracting(OAuth2Error::getDescription).containsExactly(AUDIENCE_MISSING_ERROR);
        assertThat(output.getOut())
                .contains(REJECTED_LOG)
                .contains(ISSUER, SUBJECT, CLIENT_ID, context.name(), RESOURCE_NAME);
    }

    private static Stream<Arguments> allModesWithAllContexts() {
        return Stream.of(StrictAudienceValidationMode.values()).flatMap(mode ->
                Stream.of(JeapAuthenticationContext.values()).map(context -> Arguments.of(mode, context)));
    }

    private static Stream<Arguments> allModesWithAudienceCheckedContexts() {
        return Stream.of(StrictAudienceValidationMode.values()).flatMap(mode ->
                Stream.of(USER, SYS).map(context -> Arguments.of(mode, context)));
    }

    private static Stream<Arguments> allModesWithMissingOrEmptyAudience() {
        return Stream.of(StrictAudienceValidationMode.values()).flatMap(mode ->
                Stream.of(AUDIENCE_MISSING, AUDIENCE_EMPTY).map(audience -> Arguments.of(mode, audience)));
    }

    private static Stream<Arguments> audienceCheckedContextsWithMissingOrEmptyAudience() {
        return Stream.of(USER, SYS).flatMap(context ->
                Stream.of(AUDIENCE_MISSING, AUDIENCE_EMPTY).map(audience -> Arguments.of(context, audience)));
    }

    private static Jwt createJwt(Collection<String> audience, JeapAuthenticationContext context) {
        Jwt.Builder builder = Jwt.withTokenValue("dummy-value")
                .header("dummy-header", "dummy-value")
                .issuer(ISSUER)
                .subject(SUBJECT)
                .claim("clientId", CLIENT_ID)
                .claim(JeapAuthenticationContext.getContextJwtClaimName(), context.name());
        if (audience != null) {
            builder.audience(audience);
        }
        return builder.build();
    }

}
