package ch.admin.bit.jeap.security.resource.introspection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.HashMap;
import java.util.Map;

/**
 * Introspecting a JSON Web Token (non-opaque) using the token introspector configured for a token's issuer
 */
@Slf4j
class JeapJwtIntrospector {

    private final Map<String, JeapTokenIntrospector> issuerTokenIntrospectors = new HashMap<>();

    /**
     * Construct a JeapJwtIntrospector instance from the given issuer to introspector mappings.
     *
     * @param issuerTokenIntrospectors The issuer to introspector mappings
     */
    JeapJwtIntrospector( Map<String, JeapTokenIntrospector> issuerTokenIntrospectors) {
        this.issuerTokenIntrospectors.putAll(issuerTokenIntrospectors);
    }

    /**
     * Introspects a JWT using the token introspector configured for the token's issuer.
     *
     * @param jwt The JSON Web Token to introspect
     * @return A map of attributes returned by the introspector
     * @throws JeapIntrospectionInvalidTokenException if the token is not valid
     * @throws JeapIntrospectionException if the introspection failed
     */
    Map<String, Object> introspect(Jwt jwt) throws JeapIntrospectionException, JeapIntrospectionInvalidTokenException {
        try {
            return getTokenIntrospector(jwt).introspect(jwt.getTokenValue());
        } catch (JeapIntrospectionException jie) {
            log.error("Introspection failed for a token from issuer '{}' for subject '{}'.", jwt.getIssuer(), jwt.getSubject(), jie);
            throw jie; // rethrow to keep the exact exception (sub) type
        } catch (Exception e) {
            String msg = "An error occurred while introspecting a token from issuer '%s' for subject '%s'.".formatted(jwt.getIssuer(), jwt.getSubject());
            log.error(msg, e);
            throw new JeapIntrospectionException(msg, e);
        }
    }

    private JeapTokenIntrospector getTokenIntrospector(Jwt jwt) {
        final String issuer = jwt.getIssuer().toString();
        JeapTokenIntrospector tokenIntrospector = issuerTokenIntrospectors.get(issuer);
        if (tokenIntrospector == null) {
            throw new JeapIntrospectionUnknownIssuerException(issuer, "No token introspector configured for the issuer.");
        }
        return tokenIntrospector;
    }

}
