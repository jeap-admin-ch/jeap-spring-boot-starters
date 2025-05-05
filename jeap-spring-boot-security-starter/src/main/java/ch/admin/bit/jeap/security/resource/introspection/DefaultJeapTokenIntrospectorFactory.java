package ch.admin.bit.jeap.security.resource.introspection;

public class DefaultJeapTokenIntrospectorFactory implements JeapTokenIntrospectorFactory {
    @Override
    public JeapTokenIntrospector create(JeapTokenIntrospectorConfiguration config) {
        return new DelegatingToSpringJeapTokenIntrospector(config);
    }
}
