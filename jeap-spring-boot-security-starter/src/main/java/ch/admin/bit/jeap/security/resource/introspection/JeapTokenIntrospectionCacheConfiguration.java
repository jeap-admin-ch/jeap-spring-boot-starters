package ch.admin.bit.jeap.security.resource.introspection;

import java.time.Duration;

/**
 * Configuration of the token introspection cache of an authorization server.
 *
 * @param issuer      The issuer of the authorization server the cache belongs to
 * @param maximumSize The maximum number of introspection responses kept in the cache
 * @param timeToLive  The maximum time a cached introspection response is kept
 */
public record JeapTokenIntrospectionCacheConfiguration(
        String issuer,
        long maximumSize,
        Duration timeToLive
) {}
