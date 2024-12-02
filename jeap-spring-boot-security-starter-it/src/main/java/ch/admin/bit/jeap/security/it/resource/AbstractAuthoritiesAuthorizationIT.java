package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.token.AuthoritiesResolver;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

@Import(AbstractAuthoritiesAuthorizationIT.IntegrationTestConfiguration.class)
public class AbstractAuthoritiesAuthorizationIT extends AccessTokenITBase {

    private final String SUBJECT = "69368608-D736-43C8-5F76-55B7BF168299";

    protected AbstractAuthoritiesAuthorizationIT(int serverPort, String context) {
        super(serverPort, context);
    }

    @TestConfiguration
    public static class IntegrationTestConfiguration {

        @Bean
        public AuthoritiesResolver testAuthoritiesResolver() {
            return (userRoles, businessPartnerRoles) -> {
                if (userRoles.contains("admin")) {
                    return Set.of(new SimpleGrantedAuthority("resource:write"));
                }
                return Collections.emptySet();
            };
        }
    }

    protected void testGetAuth_whenApplicationResolvesAdminAuthorities_thenAccessGranted() {
        final String jeapToken = createJeapTokenForUserRoles("admin");
        assertHttpStatusWithTokenOnGet(authoritiesAuthPathSpec, jeapToken, HttpStatus.OK)
                .body("authorities", hasItem(equalTo("resource:write")));
        assertHttpStatusWithTokenOnGet(authoritiesProgrammaticAuthPathSpec, jeapToken, HttpStatus.OK);
    }

    protected void testGetAuth_whenApplicationResolvesNoAuthorities_thenAccessRejected() {
        final String jeapToken = createJeapTokenForUserRoles("user");
        assertHttpStatusWithTokenOnGet(authoritiesAuthPathSpec, jeapToken, HttpStatus.FORBIDDEN);
        assertHttpStatusWithTokenOnGet(authoritiesProgrammaticAuthPathSpec, jeapToken, HttpStatus.FORBIDDEN);
    }

    private String createJeapTokenForUserRoles(String... roles) {
        return jwsBuilderFactory.createValidForFixedLongPeriodBuilder(SUBJECT, JeapAuthenticationContext.USER).
                withUserRoles(roles).
                build().serialize();
    }
}
