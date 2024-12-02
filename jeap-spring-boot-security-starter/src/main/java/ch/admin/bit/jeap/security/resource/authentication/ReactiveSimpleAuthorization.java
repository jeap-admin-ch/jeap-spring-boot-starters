package ch.admin.bit.jeap.security.resource.authentication;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

/**
 * This class provides methods to support authorization needs based on the current security context for Spring WebFlux applications.
 */
public class ReactiveSimpleAuthorization {

    /**
     * Create a SimpleRoleRepository based on the current reactive security context. The Mono returned by this method must
     * take part in a reactive chain subscribed by a web endpoint. Otherwise, the reactive security context won't be
     * accessible and an exception will result.
     *
     * @return A {@link SimpleRoleRepository} based on the roles in the current reactive security context.
     */
    public Mono<SimpleRoleRepository> getSimpleRoleRepository() {
        return getJeapAuthenticationToken().map(token -> new SimpleRoleRepository(token.getUserRoles(), token.getBusinessPartnerRoles()));
    }

    /**
     * Fetch the JeapAuthenticationToken from the current security context. The Mono returned by this method must
     * take part in a reactive chain subscribed by a web endpoint. Otherwise, the reactive security context won't be
     * accessible and an exception will result.
     *
     * @return The {@link JeapAuthenticationToken} from the current reactive security context.
     */
    public Mono<JeapAuthenticationToken> getJeapAuthenticationToken() {
        return ReactiveSecurityContextHolder.getContext().
                map(SecurityContext::getAuthentication).
                cast(JeapAuthenticationToken.class);
    }

}
