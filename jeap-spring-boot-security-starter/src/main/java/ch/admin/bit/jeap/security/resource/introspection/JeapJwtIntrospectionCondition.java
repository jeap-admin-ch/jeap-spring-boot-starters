package ch.admin.bit.jeap.security.resource.introspection;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Condition to checksif a JWT token is lightweight i.e., does not contain all the data and therefore
 * needs to be introspected to get all the data.
 */
public interface JeapJwtIntrospectionCondition {

    boolean needsIntrospection(Jwt jwt);

}
