package ch.admin.bit.jeap.security.resource.introspection;

import java.util.Map;

/**
 * Interface for introspecting a possibly opaque token.
 */
public interface JeapTokenIntrospector {

    /**
     * Introspects a possibly opaque token and returns the attributes returned by the introspection endpoint.
     *
     * @param token A possibly opaque token
     * @return A map of attributes returned by the introspection endpoint
     * @throws JeapIntrospectionInvalidTokenException if the token is not valid
     * @throws JeapIntrospectionException if the introspection fails
     */
    Map<String, Object> introspect(String token) throws JeapIntrospectionInvalidTokenException, JeapIntrospectionException;

}
