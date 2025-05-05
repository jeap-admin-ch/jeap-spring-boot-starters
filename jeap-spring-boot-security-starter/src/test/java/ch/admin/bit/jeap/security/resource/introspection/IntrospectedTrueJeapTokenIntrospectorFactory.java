package ch.admin.bit.jeap.security.resource.introspection;

import java.util.Map;

/**
 * This class is used for testing purposes only. It implements the JeapTokenIntrospectorFactory interface
 * and creates JeapTokenIntrospector instances that always return the attribute introspection=true.
 */
class IntrospectedTrueJeapTokenIntrospectorFactory implements JeapTokenIntrospectorFactory {

    @Override
    public JeapTokenIntrospector create(JeapTokenIntrospectorConfiguration config) {
        return IntrospectedTrueJeapTokenIntrospectorFactory::introspectedTrueAttribute;
    }

    private static Map<String, Object> introspectedTrueAttribute(String token) {
        return Map.of("active", true,"introspected", true);
    }

}
