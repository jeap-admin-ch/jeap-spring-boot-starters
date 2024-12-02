package ch.admin.bit.jeap.security.resource.validation;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AudienceJwtValidatorTest {

    private static final String RESOURCE_NAME = "resource-name";
    private static final String OTHER_AUDIENCE = "other-audience";
    private static final String YET_ANOTHER_AUDIENCE = "yet-another-audience";

    private static final List<String> AUDIENCE_INCLUDING_RESOURCE = getAudienceList(OTHER_AUDIENCE, RESOURCE_NAME, YET_ANOTHER_AUDIENCE);
    private static final List<String> AUDIENCE_EXCLUDING_RESOURCE = getAudienceList(OTHER_AUDIENCE, YET_ANOTHER_AUDIENCE);
    private static final List<String> AUDIENCE_EMPTY = Collections.emptyList();
    private static final List<String> AUDIENCE_NULL = null;

    @Test
    public void testValidateSysContext() {
        ckeckIncludingExcludingEmptyForContext(JeapAuthenticationContext.SYS, true, false, true);
    }

    @Test
    public void testValidateUserContext() {
        ckeckIncludingExcludingEmptyForContext(JeapAuthenticationContext.USER, true, false, true);
    }

    @Test
    public void testValidateB2bContext() {
        ckeckIncludingExcludingEmptyForContext(JeapAuthenticationContext.B2B, true, true, true);
    }

    public void ckeckIncludingExcludingEmptyForContext(JeapAuthenticationContext context, boolean expectedResultIncluding, boolean expectedResultExcluding, boolean expectedResultEmpty) {
        check(createAudienceJwtValidatorForAudience(RESOURCE_NAME), createJwt(AUDIENCE_INCLUDING_RESOURCE, context), expectedResultIncluding,
              createFailInfo(RESOURCE_NAME, AUDIENCE_INCLUDING_RESOURCE, context, expectedResultIncluding));

        check(createAudienceJwtValidatorForAudience(RESOURCE_NAME), createJwt(AUDIENCE_EXCLUDING_RESOURCE, context), expectedResultExcluding,
              createFailInfo(RESOURCE_NAME, AUDIENCE_EXCLUDING_RESOURCE, context, expectedResultExcluding));

        check(createAudienceJwtValidatorForAudience(RESOURCE_NAME), createJwt(AUDIENCE_EMPTY, context), expectedResultEmpty,
              createFailInfo(RESOURCE_NAME, AUDIENCE_EMPTY, context, expectedResultEmpty));
        check(createAudienceJwtValidatorForAudience(RESOURCE_NAME), createJwt(AUDIENCE_NULL, context), expectedResultEmpty,
                createFailInfo(RESOURCE_NAME, AUDIENCE_NULL, context, expectedResultEmpty));

    }

    private String createFailInfo(String resourceName, List<String> jwtAudiences, JeapAuthenticationContext context, boolean expectedResult) {
        return String.format("Expected audience validation to %s for resource '%s' in context '%s' for JWT audience claim '[%s]'.",  expectedResult ? "succeed" : "fail",
                              resourceName, context, jwtAudiences != null ? jwtAudiences.stream().collect(Collectors.joining(", ")) : "null");
    }

    private void check(AudienceJwtValidator validator, Jwt jwt, boolean successful, String failureInfo) {
        Assertions.assertEquals(successful, !validator.validate(jwt).hasErrors(), failureInfo);
    }

    private static List<String> getAudienceList(String... audiences) {
        return audiences != null ? Arrays.asList(audiences) : Collections.emptyList();
    }

    private AudienceJwtValidator createAudienceJwtValidatorForAudience(String audience) {
        return new AudienceJwtValidator(audience);
    }

    private Jwt createJwt(Collection<String> audience, JeapAuthenticationContext context) {
        Jwt.Builder builder = Jwt.withTokenValue("dummy-value").header("dummy-header", "dummy-value");
        if (audience != null) {
            builder.audience(audience);
        }
        if (context != null) {
            builder.claim(JeapAuthenticationContext.getContextJwtClaimName(), context.name());
        }

        return builder.build();
    }

}
