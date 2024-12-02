package ch.admin.bit.jeap.security.resource.authentication;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import lombok.experimental.Delegate;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * This class provides methods to support authorization needs based on the current security context for Spring WebMvc applications.
 */
public class ServletSimpleAuthorization {

    /**
     * All methods from {@link SimpleRoleRepository} can be used directly on this class as they are
     * delegated to a new instance of {@link SimpleRoleRepository}
     */
    @Delegate
    private SimpleRoleRepository getSimpleRoleRepository() {
        JeapAuthenticationToken jeapAuthenticationToken = getJeapAuthenticationToken();
        return new SimpleRoleRepository(jeapAuthenticationToken.getUserRoles(), jeapAuthenticationToken.getBusinessPartnerRoles());
    }

    /**
     * Fetch the JeapAuthenticationToken from the current security context.
     *
     * @return The JeapAuthenticationToken extracted from the current security context.
     */
    public JeapAuthenticationToken getJeapAuthenticationToken() {
        return (JeapAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    }

}
