package ch.admin.bit.jeap.security.resource.introspection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class JeapJwtIntrospection {

    private final JeapJwtIntrospector jwtIntrospector;
    private final JeapJwtIntrospectionCondition introspectionCondition;

    /**
     * Introspect a JWT if needed and enrich it with the attributes returned by the introspection.
     *
     * @param jwt The JWT to be introspected and enriched if needed
     * @return The JWT possibly enriched with the attributes returned by the introspection of the token
     * @throws JeapIntrospectionInvalidTokenException if the token is not valid
     * @throws JeapIntrospectionException if the introspection failed
     */
    public Jwt introspectIfNeeded(Jwt jwt) throws JeapIntrospectionInvalidTokenException, JeapIntrospectionException {
        if (introspectionCondition.needsIntrospection(jwt)) {
            return introspectAndEnrich(jwt);
        } else {
            return jwt;
        }
    }

    /**
     * Check if the JWT is valid by introspecting it.
     *
     * @param jwt The JWT to be checked
     * @return true if the JWT is valid, false otherwise (including if the introspection failed)
     */
    public boolean isValid(Jwt jwt) {
        try {
            jwtIntrospector.introspect(jwt);
            // If no exception is thrown, the token is valid
            return true;
        } catch (JeapIntrospectionInvalidTokenException e) {
            log.error("Introspection found an invalid token from issuer '{}' for subject '{}'.", jwt.getIssuer(), jwt.getSubject(), e);
            return false;
        } catch (Exception e) {
            log.error("Declaring a token from issuer '{}' for subject '{}' to be invalid as its introspection failed.", jwt.getIssuer(), jwt.getSubject(), e);
            return false;
        }
    }

    private Jwt introspectAndEnrich(Jwt jwt) {
        Map<String, Object> introspectionAttributes = jwtIntrospector.introspect(jwt);
        Map<String, Object> jwtClaims = new HashMap<>(jwt.getClaims());
        introspectionAttributes.forEach( (attribute, value) ->
                // Don't overwrite existing claims, only add new ones.
                // -> This keeps normalizations applied to claims by spring security/jose intact.
                jwtClaims.computeIfAbsent(attribute, k -> value));
        return new Jwt(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), jwt.getHeaders(), jwtClaims);
    }

}
