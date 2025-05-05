package ch.admin.bit.jeap.security.resource.introspection;

import java.util.Map;

/**
 * This class is used for testing purposes only. It implements the JeapTokenIntrospectorFactory interface
 * and creates JeapTokenIntrospector instances that always throw an introspection exception.
 */
class IntrospectionExceptionJeapTokenIntrospectorFactory implements JeapTokenIntrospectorFactory {

    @Override
    public JeapTokenIntrospector create(JeapTokenIntrospectorConfiguration config) {
        return IntrospectionExceptionJeapTokenIntrospectorFactory::introspectedTrueAttribute;
    }

    private static Map<String, Object> introspectedTrueAttribute(String token) {
        throw new JeapIntrospectionException("Introspection failed");
    }

}
