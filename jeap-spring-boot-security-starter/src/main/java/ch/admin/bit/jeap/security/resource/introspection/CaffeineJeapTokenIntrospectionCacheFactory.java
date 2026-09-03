package ch.admin.bit.jeap.security.resource.introspection;

/**
 * Default token introspection cache factory creating Caffeine based caches local to the instance.
 */
class CaffeineJeapTokenIntrospectionCacheFactory implements JeapTokenIntrospectionCacheFactory {

    @Override
    public JeapTokenIntrospectionCache create(JeapTokenIntrospectionCacheConfiguration config) {
        return new CaffeineJeapTokenIntrospectionCache(config);
    }

}
