package ch.admin.bit.jeap.security.resource.introspection;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class DefaultJeapTokenIntrospectorFactory implements JeapTokenIntrospectorFactory {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<JeapTokenIntrospectionMetrics> jeapTokenIntrospectionMetrics;

    @Override
    public JeapTokenIntrospector create(JeapTokenIntrospectorConfiguration config) {
        JeapTokenIntrospector introspector = new DelegatingToSpringJeapTokenIntrospector(config);
        if (jeapTokenIntrospectionMetrics.isPresent()) {
            return jeapTokenIntrospectionMetrics.get().timeTokenIntrospectionRequests(introspector, config.issuer());
        } else {
           return introspector;
        }
    }

}
