package ch.admin.bit.jeap.security.resource.introspection;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Cache for the responses of a token introspection endpoint. Implementations are created per authorization server by a
 * {@link JeapTokenIntrospectionCacheFactory} and must be safe for concurrent use.
 * <p>
 * Both operations bring the cache up to date with the result of an introspection they execute: a response returned
 * normally by the introspection (the token has been reported active) replaces the cached response of the token, a
 * token reported inactive ({@link JeapIntrospectionInvalidTokenException}) removes the cached response, and any other
 * failure leaves the cached response untouched, as it carries no new information about the token. Exceptions thrown
 * by an introspection are always propagated to the caller. Implementations must make sure that the result of an older
 * introspection never overwrites the result of a newer one, e.g. by executing the introspections of a token one after
 * the other.
 * <p>
 * A cached response is shared by all requests presenting the same token. Implementations must therefore return
 * responses that cannot be modified by the caller, nested maps and collections included. Implementations must not
 * serve a cached response beyond the expiry of the token or beyond the expiry declared by the response itself
 * (attribute 'exp', see RFC 7662).
 */
public interface JeapTokenIntrospectionCache {

    /**
     * Return the cached introspection response for the given token if there is one. Otherwise, introspect the token
     * with the given introspection, update the cache as described above and return the response.
     *
     * @param key           The cache key identifying the token
     * @param jwt           The token to introspect, e.g. for bounding the lifetime of the cache entry by the token's expiry
     * @param introspection Introspects the token on the introspection endpoint
     * @return The attributes returned by the introspection endpoint for the token
     */
    Map<String, Object> getOrIntrospect(JeapTokenIntrospectionCacheKey key, Jwt jwt, Supplier<Map<String, Object>> introspection);

    /**
     * Introspect the given token with the given introspection, whether there is a cached response for the token or not,
     * update the cache as described above and return the response. To be used when the introspection is meant to
     * check the current validity of the token, which a cached response cannot answer.
     *
     * @param key           The cache key identifying the token
     * @param jwt           The token to introspect, e.g. for bounding the lifetime of the cache entry by the token's expiry
     * @param introspection Introspects the token on the introspection endpoint
     * @return The attributes returned by the introspection endpoint for the token
     */
    Map<String, Object> introspect(JeapTokenIntrospectionCacheKey key, Jwt jwt, Supplier<Map<String, Object>> introspection);

}
