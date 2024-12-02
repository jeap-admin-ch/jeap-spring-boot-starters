package ch.admin.bit.jeap.security.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;

import java.util.Set;

/**
 * This bean postprocessor implements a default for the scope configuration properties of clients in the Spring Boot
 * client registry. It replaces an empty scope configuration with the scope 'openid', which is mandatory for OIDC auth flows.
 * Defaulting the scope property to 'openid' makes it unnecessary to repeatedly configure such a scope for every client.
 *
 * This bean postprocessor has been added because of a a problem with the current Spring Security implementation. If an
 * OAuth2 client does not specify a scope in its client configuration, Spring Security will add certain scopes to its requests
 * to the authorization server on its own. More precisely, Spring Security will add all scopes that the authorization server
 * lists at its configuration endpoint e.g. at issuer/.well-known/openid-configuration. This does not make sense, especially
 * if those scopes include the scope 'offline_access', which makes the authorization server needlessly accumulate
 * long lived refresh tokens.
 *
 * Spring Security 5.4 changes the scope handling by no longer fetching scopes from the authorization server's configuration
 * endpoint and leaving an empty scope configuration as is. However, this might cause problems for OIDC (but not for 'pure' OAuth2),
 * because of the missing mandatory 'openid' scope. See https://github.com/spring-projects/spring-security/issues/8514 for details.
 * When upgrading the jeap-spring-boot-startes to a newer Spring Boot version with a newer Spring Security version than the
 * current 5.3 version, we might want to reconsider the situation.
 */
@Slf4j
@AutoConfiguration
public class OAuth2ClientRegistryPostprocessor implements BeanPostProcessor {

    private static final String OPENID_SCOPE = "openid";

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof OAuth2ClientProperties) {
            OAuth2ClientProperties properties = (OAuth2ClientProperties) bean;
            properties.getRegistration().forEach(this::defaultToOpenidScopeForEmptyScope);
        }
        return bean;
    }

    private void defaultToOpenidScopeForEmptyScope(String registrationId, OAuth2ClientProperties.Registration registration) {
        if ((registration.getScope() == null) || registration.getScope().isEmpty()) {
            log.info("Client registration with id '{}' did not specify a scope. Setting scope to '{}'.", registrationId, OPENID_SCOPE);
            registration.setScope(Set.of(OPENID_SCOPE));
        }
        else {
            log.debug("Client registration with id '{}' requests the following scopes '{}'.", registrationId, String.join(", ", registration.getScope()));
        }
    }

}
