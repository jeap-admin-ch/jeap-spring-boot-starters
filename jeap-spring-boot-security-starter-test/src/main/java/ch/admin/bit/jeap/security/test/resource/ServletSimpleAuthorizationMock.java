package ch.admin.bit.jeap.security.test.resource;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.resource.authentication.ServletSimpleAuthorization;
import lombok.Builder;
import lombok.Setter;
import lombok.Singular;

import java.util.Map;
import java.util.Set;

/**
 * An instance of this class can serve as a mock for a ServletSimpleAuthorization instance.
 * It may be convenient to use this class instead of e.g. Mockito mocks in unit tests.
 */
public class ServletSimpleAuthorizationMock extends ServletSimpleAuthorization {

    @Setter
    private JeapAuthenticationToken jeapAuthenticationToken;

    /**
     * Create a new mock for which the authentication needs to be set later.
     */
    public ServletSimpleAuthorizationMock() {
        this.jeapAuthenticationToken = null;
    }

    /**
     * Create a new mock that derives authorization from the given authentication.
     *
     * @param jeapAuthenticationToken The authentication.
     */
    public ServletSimpleAuthorizationMock(JeapAuthenticationToken jeapAuthenticationToken) {
        this.jeapAuthenticationToken = jeapAuthenticationToken;
    }

    /**
     * Create a mock instance based on an authentication that includes the given roles.
     *
     * @param userRoles            User roles to include.
     * @param businessPartnerRoles Business partner roles to include
     * @return The mock
     */
    @Builder
    private static ServletSimpleAuthorizationMock build(@Singular Set<String> userRoles,
                                                        @Singular Map<String, Set<String>> businessPartnerRoles) {
        JeapAuthenticationTestTokenBuilder builder = JeapAuthenticationTestTokenBuilder.create()
                .withUserRoles(userRoles.toArray(String[]::new));
        businessPartnerRoles.forEach((key, value) -> builder.withBusinessPartnerRoles(key, value.toArray(String[]::new)));
        return new ServletSimpleAuthorizationMock(builder.build());
    }

    @Override
    public JeapAuthenticationToken getJeapAuthenticationToken() {
        // Just return the configured mock authentication token (i.e. don't fetch it from the current security context).
        return jeapAuthenticationToken;
    }

}
