package ch.admin.bit.jeap.security.resource.validation;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;

import java.util.*;

/**
 * This class implements a JwtValidator that checks if the access token for a given context has been issued by
 * a given issuer.
 */
@Slf4j
public class ContextIssuerJwtValidator implements OAuth2TokenValidator<Jwt> {
    private final JwtIssuerValidator jwtIssuerValidator;
    private final List<JeapAuthenticationContext> contexts;

    ContextIssuerJwtValidator(Collection<JeapAuthenticationContext> contexts, String issuer) {
        this.contexts = new ArrayList<>(contexts);
        this.jwtIssuerValidator = new JwtIssuerValidator(issuer);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            JeapAuthenticationContext context = JeapAuthenticationContext.readFromJwt(jwt);
            if(!contexts.contains(context)) {
                return createErrorResult("Unsupported context claim value '" + context + "'.");
            } else {
                return jwtIssuerValidator.validate(jwt);
            }
        } catch (IllegalArgumentException e) {
            //This is the case if the context is not valid
            return createErrorResult(e.getMessage());
        }
    }

    private OAuth2TokenValidatorResult createErrorResult(String errorMessage) {
        OAuth2Error error = new OAuth2Error("invalid_token", errorMessage, null);
        log.warn(error.getDescription());
        return OAuth2TokenValidatorResult.failure(error);
    }
}
