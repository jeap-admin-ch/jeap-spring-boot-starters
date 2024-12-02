package ch.admin.bit.jeap.security.resource.semanticAuthentication;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Support for authorization on current security context for Spring MVC applications.
 */
@RequiredArgsConstructor
public class ServletSemanticAuthorization {
    private final String systemName;

    /**
     * All methods from {@link SemanticRoleRepository} can be used directly on this class as they are
     * delegated to a new instance of {@link SemanticRoleRepository}
     */
    @Delegate
    private SemanticRoleRepository getSemanticRoleRepository() {
        return new SemanticRoleRepository(systemName, getAuthenticationToken());
    }

    public JeapAuthenticationToken getAuthenticationToken() {
        return (JeapAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    }
}
