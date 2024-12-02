package ch.admin.bit.jeap.security.resource.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@RequiredArgsConstructor
public class JeapAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String USER_ROLES_CLAIM = "userroles";
    private static final String BUSINESS_PARTNER_ROLES_CLAIM = "bproles";

    private final AuthoritiesResolver authoritiesResolver;

    public JeapAuthenticationConverter() {
        this(new DefaultAuthoritiesResolver());
    }

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Set<String> userRoles = extractUserRoles(jwt);
        Map<String, Set<String>> businessPartnerRoles = extractBusinessPartnerRoles(jwt);
        Collection<GrantedAuthority> grantedAuthorities = authoritiesResolver.deriveAuthoritiesFromRoles(userRoles, businessPartnerRoles);
        return new JeapAuthenticationToken(jwt, userRoles, businessPartnerRoles, grantedAuthorities);
    }

    private Set<String> extractUserRoles(Jwt jwt) {
        List<String> userRolesClaim = Optional.of(jwt)
                .map(Jwt::getClaims)
                .map(map -> map.get(USER_ROLES_CLAIM))
                .flatMap(this::castUserRoles)
                .orElse(Collections.emptyList());

        return Set.copyOf(userRolesClaim);
    }

    private Map<String, Set<String>> extractBusinessPartnerRoles(Jwt jwt) {
        return extractBusinesspartnerRolesClaim(jwt)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
    }

    private Map<String, List<String>> extractBusinesspartnerRolesClaim(Jwt jwt) {
        return Optional.of(jwt)
                .map(Jwt::getClaims)
                .map(map -> map.get(BUSINESS_PARTNER_ROLES_CLAIM))
                .flatMap(this::castBusinessPartnerRoleMap)
                .orElse(Collections.emptyMap());
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, List<String>>> castBusinessPartnerRoleMap(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of((Map<String, List<String>>) value);
        } catch (ClassCastException e) {
            log.warn("Unable to cast Business Partner Role claim to Map<List<String>>, ignoring the entry.");
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<List<String>> castUserRoles(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of((List<String>) value);
        } catch (ClassCastException e) {
            log.warn("Unable to cast User Role claim to List<String>, ignoring the entry.");
            return Optional.empty();
        }
    }
}
