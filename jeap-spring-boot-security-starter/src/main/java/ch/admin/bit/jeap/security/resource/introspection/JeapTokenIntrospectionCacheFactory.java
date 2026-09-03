package ch.admin.bit.jeap.security.resource.introspection;

/**
 * Factory for the token introspection caches of the authorization servers with caching enabled. Provide an
 * implementation as a Spring bean to replace the default (Caffeine based) cache implementation.
 */
public interface JeapTokenIntrospectionCacheFactory {

    JeapTokenIntrospectionCache create(JeapTokenIntrospectionCacheConfiguration config);

}
