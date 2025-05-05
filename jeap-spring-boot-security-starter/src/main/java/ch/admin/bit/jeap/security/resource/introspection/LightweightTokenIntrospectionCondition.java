package ch.admin.bit.jeap.security.resource.introspection;

import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * This condition checks if a JWT token is lightweight i.e., does not contain all the data and therefore
 * needs to be introspected to get all the data. A token is considered to be lightweight by this condition
 * if the claim "roles_pruned_chars" is present or the scope "lightweight" is present.
 */
public class LightweightTokenIntrospectionCondition implements JeapJwtIntrospectionCondition {

    static final String ROLES_PRUNED_CHARS_CLAIM = "roles_pruned_chars";
    static final String LIGHTWEIGHT_SCOPE_NAME = "lightweight";
    private static final Pattern LIGHTWEIGHT_SCOPE_PATTERN = Pattern.compile("\\b" + LIGHTWEIGHT_SCOPE_NAME + "\\b");

    @Override
    public boolean needsIntrospection(Jwt jwt) {
        Map<String, Object> claims = jwt.getClaims();
        // Check if roles have been pruned
        if (claims.containsKey(ROLES_PRUNED_CHARS_CLAIM)) {
            return true;
        }
        // Check for the lightweight scope
        Object scopeObject = claims.get(OAuth2TokenIntrospectionClaimNames.SCOPE);
        if (scopeObject instanceof String scope) { // scope is a single string (as in the OAuth2 standard)
            return LIGHTWEIGHT_SCOPE_PATTERN.matcher(scope).find();
        } else if (scopeObject instanceof String[] scopes) { // scope is an array (after Spring Security normalization)
            return Arrays.asList(scopes).contains(LIGHTWEIGHT_SCOPE_NAME);
        }

        return false;
    }

}
