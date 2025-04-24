package ch.admin.bit.jeap.security.resource.introspection;

public interface JeapTokenIntrospectorFactory {
    JeapTokenIntrospector create(JeapTokenIntrospectorConfiguration config);
}
