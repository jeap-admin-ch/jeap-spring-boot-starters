package ch.admin.bit.jeap.security.test.resource;

import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.ServletSemanticAuthorization;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import lombok.Builder;
import lombok.NonNull;
import lombok.Setter;
import lombok.Singular;

import java.util.Map;
import java.util.Set;

/**
 * This class can serve as a mock for {@link ServletSemanticAuthorization},
 * it can be used instead of e.g. Mockito mocks in unit tests.
 */
@SuppressWarnings("WeakerAccess")
public class ServletSemanticAuthorizationMock extends ServletSemanticAuthorization {

    @Setter
    private JeapAuthenticationToken authenticationToken;

    public ServletSemanticAuthorizationMock(String systemName, JeapAuthenticationToken authenticationToken) {
        this(systemName);
        this.authenticationToken = authenticationToken;
    }

    public ServletSemanticAuthorizationMock(String systemName) {
        super(systemName);
    }

    /**
     * Create a mock instance with an authentication that includes the given roles.
     *
     * @param userRoles            The user roles to include into the authentication.
     * @param businessPartnerRoles Roles for a given business partner to include into the authentication
     * @param systemName           The name of the system
     * @return The mock
     */
    @Builder
    private static ServletSemanticAuthorizationMock build(@Singular Set<SemanticApplicationRole> userRoles,
                                                          @Singular Map<String, Set<SemanticApplicationRole>> businessPartnerRoles,
                                                          @NonNull String systemName) {
        JeapAuthenticationTestTokenBuilder builder = JeapAuthenticationTestTokenBuilder.create()
                .withUserRoles(userRoles);
        businessPartnerRoles.forEach(builder::withBusinessPartnerRoles);
        return new ServletSemanticAuthorizationMock(systemName, builder.build());
    }

    @Override
    public JeapAuthenticationToken getAuthenticationToken() {
        return authenticationToken;
    }

}
