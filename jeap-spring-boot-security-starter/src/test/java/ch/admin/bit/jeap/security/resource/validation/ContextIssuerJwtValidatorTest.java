package ch.admin.bit.jeap.security.resource.validation;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.List;

import static ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext.*;

class ContextIssuerJwtValidatorTest {

    @Test
    void testValidate_whenNoContextIssuersConfigured_thenError() {
        ContextIssuerJwtValidator validator = new ContextIssuerJwtValidator(Collections.emptyList(), "issuer");
        Jwt jwt = createJwt(SYS, "issuer");

        validator.validate(jwt);

        Assertions.assertTrue(validator.validate(jwt).hasErrors());
    }

    @Test
    void testValidate_whenContextMissing_thenError() {
        ContextIssuerJwtValidator validator = new ContextIssuerJwtValidator(List.of(SYS, USER, B2B), "issuer");
        Jwt jwt = createJwt(null, "some-issuer");

        validator.validate(jwt);

        Assertions.assertTrue(validator.validate(jwt).hasErrors());
    }

    @Test
    void testValidate_whenInvalidContext_thenError() {
        ContextIssuerJwtValidator validator = new ContextIssuerJwtValidator(List.of(SYS, USER, B2B), "issuer");
        Jwt jwt = Jwt.withTokenValue("dummy-value").header("dummy-header", "dummy-value").
                claim(JeapAuthenticationContext.getContextJwtClaimName(), "invalid-context").
                issuer("some-issuer").build();

        validator.validate(jwt);

        Assertions.assertTrue(validator.validate(jwt).hasErrors());
    }

    @Test
    void testValidate_whenUnsupportedContext_thenError() {
        ContextIssuerJwtValidator validator = new ContextIssuerJwtValidator(List.of(SYS, USER), "issuer");
        Jwt jwt = createJwt(B2B, "issuer");

        validator.validate(jwt);

        Assertions.assertTrue(validator.validate(jwt).hasErrors());
    }

    @Test
    void testValidate_whenIssuerDoesNotMatchContext_thenError() {
        ContextIssuerJwtValidator validator = new ContextIssuerJwtValidator(List.of(SYS, USER, B2B), "issuer");
        Jwt jwt = createJwt(SYS, "wrong-issuer-for-SYS");

        validator.validate(jwt);

        Assertions.assertTrue(validator.validate(jwt).hasErrors());
    }

    @Test
    void testValidate_whenIssuerMatchesContext_thenSuccess() {
        ContextIssuerJwtValidator validator = new ContextIssuerJwtValidator(List.of(SYS, USER, B2B), "issuer");
        Jwt jwt = createJwt(USER, "issuer");

        validator.validate(jwt);

        Assertions.assertFalse(validator.validate(jwt).hasErrors());
    }

    private Jwt createJwt(JeapAuthenticationContext context, String issuer) {
        Jwt.Builder builder = Jwt.withTokenValue("dummy-value").header("dummy-header", "dummy-value").claim("dummy-claim", "dummy-claim");
        if (context != null) {
            builder.claim(JeapAuthenticationContext.getContextJwtClaimName(), context.name());
        }
        if (issuer != null) {
            builder.issuer(issuer);
        }

        return builder.build();
    }

}
