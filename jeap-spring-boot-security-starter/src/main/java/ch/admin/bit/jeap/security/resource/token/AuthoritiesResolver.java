package ch.admin.bit.jeap.security.resource.token;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface AuthoritiesResolver {
    Collection<GrantedAuthority> deriveAuthoritiesFromRoles(Set<String> userRoles, Map<String, Set<String>> businessPartnerRoles);
}
