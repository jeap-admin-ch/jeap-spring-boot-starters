package ch.admin.bit.jeap.security.resource.token;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JeapAuthenticationToken extends JwtAuthenticationToken {

    private final Map<String, Set<String>> businessPartnerRoles;
    private final Set<String> userRoles;

    public JeapAuthenticationToken(Jwt jwt, Set<String> userRoles, Map<String, Set<String>> businessPartnerRoles, Collection<? extends GrantedAuthority> grantedAuthorities) {
        super(jwt, grantedAuthorities);
        this.businessPartnerRoles = Collections.unmodifiableMap(businessPartnerRoles);
        this.userRoles = Collections.unmodifiableSet(userRoles);
    }

    /**
     * Get the client id specified in this token.
     *
     * @return The client id specified in this token.
     */
    public String getClientId() {
        return getToken().getClaimAsString("clientId");
    }

    /**
     * Get the ext id specified in this token.
     *
     * @return The ext id specified in this token.
     */
    public String getTokenExtId() {
        return getToken().getClaimAsString("ext_id");
    }

    /**
     * Get the admin dir uid specified in this token.
     *
     * @return The admin dir uid specified in this token.
     */
    public String getAdminDirUID() {
        return getToken().getClaimAsString("admin_dir_uid");
    }

    /**
     * Get the name specified in this token.
     *
     * @return The name specified in this token.
     */
    public String getTokenName() {
        return getToken().getClaimAsString("name");
    }

    /**
     * Get the given name specified in this token.
     *
     * @return The given name specified in this token.
     */
    public String getTokenGivenName() {
        return getToken().getClaimAsString("given_name");
    }

    /**
     * Get the family name specified in this token.
     *
     * @return The family name specified in this token.
     */
    public String getTokenFamilyName() {
        return getToken().getClaimAsString("family_name");
    }

    /**
     * Get the preferred username specified in this token.
     *
     * @return The preferred username specified in this token.
     */
    public String getPreferredUsername() {
        return getToken().getClaimAsString("preferred_username");
    }

    /**
     * Get the subject specified in this token.
     *
     * @return The subject specified in this token.
     */
    public String getTokenSubject() {
        return getToken().getClaimAsString("sub");
    }

    /**
     * Get the locale specified in this token.
     *
     * @return The locale specified in this token.
     */
    public String getTokenLocale() {
        return getToken().getClaimAsString("locale");
    }

    /**
     * Get the jeap authentication context specified in this token.
     *
     * @return The jeap authentication context specified in this token.
     */
    public JeapAuthenticationContext getJeapAuthenticationContext() {
        return JeapAuthenticationContext.readFromJwt(getToken());
    }

    /**
     * Get the business partner roles listed in this token.
     *
     * @return The business partner roles grouped in sets by business partner id.
     */
    public Map<String, Set<String>> getBusinessPartnerRoles() {
        return businessPartnerRoles;
    }

    /**
     * Get the user roles listed in this token.
     *
     * @return The user roles
     */
    public Set<String> getUserRoles() {
        return userRoles;
    }

    @Override
    public String toString() {
        return String.format(
                "JeapAuthenticationToken{ subject (calling user): %s, client (calling system): %s, context: %s, authorities (all roles): %s, user roles: %s, business partner roles: %s }",
                getName(), getClientId(), getJeapAuthenticationContext(), authoritiesToString(), userRolesToString(), businessPartnerRolesToString());
    }

    private String authoritiesToString() {
        return getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> "'" + a + "'")
                .collect(Collectors.joining(","));
    }

    private String businessPartnerRolesToString() {
        return getBusinessPartnerRoles().entrySet().stream()
                .map(e -> e.getKey() + " [ " + e.getValue().stream().map(r -> "'" + r + "'").collect(Collectors.joining(", ")) + " ]")
                .collect(Collectors.joining(", "));
    }

    private String userRolesToString() {
        return getUserRoles().stream().
                map(r -> "'" + r + "'").
                collect(Collectors.joining(", "));
    }

}
