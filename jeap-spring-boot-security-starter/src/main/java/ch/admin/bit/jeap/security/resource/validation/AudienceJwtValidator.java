package ch.admin.bit.jeap.security.resource.validation;

import ch.admin.bit.jeap.security.resource.properties.StrictAudienceValidationMode;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Validates that an access token addresses this resource, i.e. that the token's 'aud' claim contains the resource id
 * configured for this resource server. Tokens in the B2B context are never audience-checked. How tokens in the USER
 * and SYS contexts that do not specify an audience are treated depends on the configured
 * {@link StrictAudienceValidationMode}. This validator must only be invoked for tokens that passed all other
 * validations (see {@link JeapJwtDecoderFactory}), so that the warnings logged in mode
 * {@link StrictAudienceValidationMode#WARN} identify exactly the tokens that are accepted in mode
 * {@link StrictAudienceValidationMode#OFF} but would be rejected in mode {@link StrictAudienceValidationMode#ON}.
 */
@Slf4j
public class AudienceJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final String INVALID_TOKEN_ERROR_CODE = "invalid_token";
    private static final String CLIENT_ID_CLAIM_NAME = "clientId";
    private static final String TOKEN_WITHOUT_AUDIENCE_DETAILS =
            "the access token from issuer '{}' for subject '{}' and client id '{}' in context '{}' does not specify an audience ('aud' claim missing or empty)";

    private final String expectedAudience;
    private final StrictAudienceValidationMode strictAudienceValidationMode;
    private final OAuth2Error audienceMismatchError;
    private final OAuth2Error audienceMissingError;

    public AudienceJwtValidator(String expectedAudience, StrictAudienceValidationMode strictAudienceValidationMode) {
        this.expectedAudience = expectedAudience;
        this.strictAudienceValidationMode = strictAudienceValidationMode;
        this.audienceMismatchError = new OAuth2Error(INVALID_TOKEN_ERROR_CODE,
                "The token is not valid for audience '" + expectedAudience + "'.", null);
        this.audienceMissingError = new OAuth2Error(INVALID_TOKEN_ERROR_CODE,
                "The token does not specify an audience and is therefore not valid for audience '" + expectedAudience + "'.", null);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (JeapAuthenticationContext.isB2B(jwt)) {
            // The B2B gateway cannot restrict the audience of the tokens it issues -> B2B tokens are never audience-checked
            return OAuth2TokenValidatorResult.success();
        }
        List<String> jwtAudience = jwt.getAudience();
        if (jwtAudience == null || jwtAudience.isEmpty()) {
            return validateTokenWithoutAudience(jwt);
        }
        if (jwtAudience.contains(expectedAudience)) {
            return OAuth2TokenValidatorResult.success();
        }
        log.warn(audienceMismatchError.getDescription());
        return OAuth2TokenValidatorResult.failure(audienceMismatchError);
    }

    private OAuth2TokenValidatorResult validateTokenWithoutAudience(Jwt jwt) {
        return switch (strictAudienceValidationMode) {
            case OFF -> OAuth2TokenValidatorResult.success(); // a token without an audience is considered valid for every resource
            case WARN -> {
                log.warn("Token without audience accepted (strict-audience-validation=warn): " + TOKEN_WITHOUT_AUDIENCE_DETAILS
                                + " and would be rejected by strict audience validation for audience '{}'.",
                        jwt.getIssuer(), jwt.getSubject(), jwt.getClaimAsString(CLIENT_ID_CLAIM_NAME),
                        jwt.getClaimAsString(JeapAuthenticationContext.getContextJwtClaimName()), expectedAudience);
                yield OAuth2TokenValidatorResult.success();
            }
            case ON -> {
                log.warn("Token without audience rejected (strict-audience-validation=on): " + TOKEN_WITHOUT_AUDIENCE_DETAILS
                                + " and is therefore not valid for audience '{}'.",
                        jwt.getIssuer(), jwt.getSubject(), jwt.getClaimAsString(CLIENT_ID_CLAIM_NAME),
                        jwt.getClaimAsString(JeapAuthenticationContext.getContextJwtClaimName()), expectedAudience);
                yield OAuth2TokenValidatorResult.failure(audienceMissingError);
            }
        };
    }

}
