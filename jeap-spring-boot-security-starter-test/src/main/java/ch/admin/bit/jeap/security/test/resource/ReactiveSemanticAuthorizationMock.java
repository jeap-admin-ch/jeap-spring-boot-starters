package ch.admin.bit.jeap.security.test.resource;

import ch.admin.bit.jeap.security.resource.semanticAuthentication.ReactiveSemanticAuthorization;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import lombok.Builder;
import lombok.NonNull;
import lombok.Setter;
import lombok.Singular;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

/**
 * This class can serve as a mock for {@link ReactiveSemanticAuthorization},
 * it can be used instead of e.g. Mockito mocks in unit tests.
 */
@SuppressWarnings("WeakerAccess")
public class ReactiveSemanticAuthorizationMock extends ReactiveSemanticAuthorization {

    @Setter
    private JeapAuthenticationToken authenticationToken;

    public ReactiveSemanticAuthorizationMock(String systemName, JeapAuthenticationToken authenticationToken) {
        this(systemName);
        this.authenticationToken = authenticationToken;
    }

    public ReactiveSemanticAuthorizationMock(String systemName) {
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
    private static ReactiveSemanticAuthorizationMock build(@Singular Set<SemanticApplicationRole> userRoles,
                                                           @Singular Map<String, Set<SemanticApplicationRole>> businessPartnerRoles,
                                                           @NonNull String systemName) {
        JeapAuthenticationTestTokenBuilder builder = JeapAuthenticationTestTokenBuilder.create().withUserRoles(userRoles);
        businessPartnerRoles.forEach(builder::withBusinessPartnerRoles);
        return new ReactiveSemanticAuthorizationMock(systemName, builder.build());
    }

    @Override
    public Mono<JeapAuthenticationToken> getAuthenticationToken() {
        return Mono.just(authenticationToken);
    }

}
