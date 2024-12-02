package ch.admin.bit.jeap.security.resource.token;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class DefaultAuthoritiesResolver implements AuthoritiesResolver {

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> deriveAuthoritiesFromRoles(Set<String> userRoles, Map<String, Set<String>> businesspartnerRoles) {
        return Stream.concat(userRoles.stream(), businesspartnerRoles.values().stream().flatMap(Set::stream))
                .map(s -> ROLE_PREFIX + s)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }
}
