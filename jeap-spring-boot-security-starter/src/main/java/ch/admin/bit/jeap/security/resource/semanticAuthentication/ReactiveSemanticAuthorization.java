package ch.admin.bit.jeap.security.resource.semanticAuthentication;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

/**
 * Support for authorization for Spring WebFlux applications. The {@link Mono}s returned must be used in a
 * reactive chain subscribed by a web endpoint. Otherwise, the reactive security context won't be accessible and an
 * exception will be thrown
 */
@RequiredArgsConstructor
public class ReactiveSemanticAuthorization {

    private final String systemName;

    /**
     * @return A {@link SemanticRoleRepository} to check for semantic roles
     */
    public Mono<SemanticRoleRepository> getSemanticRoleRepository() {
        return getAuthenticationToken().map(token -> new SemanticRoleRepository(systemName, token));
    }

    /**
     * @return Authentication token from security context.
     */
    public Mono<JeapAuthenticationToken> getAuthenticationToken() {
        return ReactiveSecurityContextHolder.getContext().
                map(SecurityContext::getAuthentication).
                cast(JeapAuthenticationToken.class);
    }

}
