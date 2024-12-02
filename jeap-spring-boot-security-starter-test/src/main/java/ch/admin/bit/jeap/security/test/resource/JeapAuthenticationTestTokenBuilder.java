package ch.admin.bit.jeap.security.test.resource;

import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.DefaultAuthoritiesResolver;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

// @jEAP team: This builder does not follow the same conventions as the Lombok builders usually used by jEAP.
// -> when breaking changes are introduced, adapt this builder to the Lombok style. See also JwsBuilder.

/**
 * This class can simplify the construction of a JeapAuthenticationToken instance for the most common use cases in unit and integration tests.
 * This class implements a builder pattern. You can start building with an empty JWT (create()) and add claims and roles to it. Or you can start
 * with a given JWT (createWithJwt()) and then add roles to it.
 */
public class JeapAuthenticationTestTokenBuilder {

    private Jwt jwt;
    private Map<String, Object> claims;
    private Set<String> userRoles;
    private Map<String, Set<String>> businessPartnerRoles;
    private Set<GrantedAuthority> authorities;

    private JeapAuthenticationTestTokenBuilder(Jwt jwt) {
        this.userRoles = new HashSet<>();
        this.authorities = null;
        this.businessPartnerRoles = new HashMap<>();
        this.claims = new HashMap<>();
        this.jwt = jwt;
    }

    public static JeapAuthenticationTestTokenBuilder create() {
        return new JeapAuthenticationTestTokenBuilder(null);
    }

    public static JeapAuthenticationTestTokenBuilder createWithJwt(Jwt jwt) {
        return new JeapAuthenticationTestTokenBuilder(jwt);
    }

    private static Jwt.Builder createDefaultJwt() {
        return Jwt.withTokenValue("dummy token value")
                // at least one header needed
                .header("alg", "none")
                // at least one claim needed
                .claim(JeapAuthenticationContext.getContextJwtClaimName(), JeapAuthenticationContext.USER.name());
    }

    public JeapAuthenticationTestTokenBuilder withContext(JeapAuthenticationContext context) {
        return withClaim(JeapAuthenticationContext.getContextJwtClaimName(), context);
    }

    public JeapAuthenticationTestTokenBuilder withExtId(String extId) {
        return withClaim("ext_id", extId);
    }

    public JeapAuthenticationTestTokenBuilder withAdminDirUID(String adminDirUID) {
        return withClaim("admin_dir_uid", adminDirUID);
    }

    public JeapAuthenticationTestTokenBuilder withName(String name) {
        return withClaim("name", name);
    }

    public JeapAuthenticationTestTokenBuilder withGivenName(String givenName) {
        return withClaim("given_name", givenName);
    }

    public JeapAuthenticationTestTokenBuilder withFamilyName(String familyName) {
        return withClaim("family_name", familyName);
    }

    public JeapAuthenticationTestTokenBuilder withPreferredUsername(String preferredUsername) {
        return withClaim("preferred_username", preferredUsername);
    }

    public JeapAuthenticationTestTokenBuilder withLocale(String locale) {
        return withClaim("locale", locale);
    }

    public JeapAuthenticationTestTokenBuilder withSubject(String subject) {
        return withClaim("sub", subject);
    }

    public JeapAuthenticationTestTokenBuilder withClaim(String claimName, Object claimValue) {
        checkNoTokenProvided();
        claims.put(claimName, claimValue);
        return this;
    }

    public JeapAuthenticationTestTokenBuilder withUserRoles(String... roles) {
        userRoles.addAll(setOf(roles));
        return this;
    }

    public JeapAuthenticationTestTokenBuilder withUserRoles(SemanticApplicationRole... roles) {
        return withUserRoles(setOf(roles));
    }

    public JeapAuthenticationTestTokenBuilder withUserRoles(Collection<SemanticApplicationRole> roles) {
        roles.stream()
                .map(SemanticApplicationRole::toString)
                .forEach(userRoles::add);
        return this;
    }

    public JeapAuthenticationTestTokenBuilder withBusinessPartnerRoles(String businessPartner, String... roles) {
        Set<String> currentRoles = businessPartnerRoles.computeIfAbsent(businessPartner, k -> new HashSet<>());
        currentRoles.addAll(setOf(roles));
        return this;
    }

    public JeapAuthenticationTestTokenBuilder withBusinessPartnerRoles(String businessPartner, SemanticApplicationRole... roles) {
        return withBusinessPartnerRoles(businessPartner, setOf(roles));
    }

    public JeapAuthenticationTestTokenBuilder withBusinessPartnerRoles(String businessPartner, Collection<SemanticApplicationRole> roles) {
        Set<String> currentRoles = businessPartnerRoles.computeIfAbsent(businessPartner, k -> new HashSet<>());
        roles.stream()
                .map(SemanticApplicationRole::toString)
                .forEach(currentRoles::add);
        return this;
    }

    public JeapAuthenticationTestTokenBuilder withAuthorities(String... authorities) {
        this.authorities = Arrays.stream(authorities).map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
        return this;
    }

    public JeapAuthenticationToken build() {
        if (authorities == null) {
            // no authorities have been configured explicitly -> derive from roles
            authorities = new HashSet<>(new DefaultAuthoritiesResolver().deriveAuthoritiesFromRoles(userRoles, businessPartnerRoles));
        }
        if (jwt != null) {
            return new JeapAuthenticationToken(jwt, userRoles, businessPartnerRoles, authorities);
        } else {
            Jwt.Builder jwtBuilder = createDefaultJwt();
            claims.forEach(jwtBuilder::claim);
            return new JeapAuthenticationToken(jwtBuilder.build(), userRoles, businessPartnerRoles, authorities);
        }
    }

    private void checkNoTokenProvided() {
        if (jwt != null) {
            throw new IllegalStateException("Token has been set explicitly, unable to add additional token claims.");
        }
    }

    private <E> Set<E> setOf(E... elements) {
        Set<E> set = new HashSet<>();
        if (elements != null) {
            set.addAll(Arrays.asList(elements));
        }
        return set;
    }
}
