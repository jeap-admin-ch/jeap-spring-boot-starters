package ch.admin.bit.jeap.security.resource.introspection;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
class JeapTokenIntrospectionMetrics {

    private static final String METRIC_INTROSPECTION = "jeap.security.token.introspection";
    private static final String METRIC_INTROSPECTION_CONDITIONAL_INTROSPECTIONS = METRIC_INTROSPECTION + ".conditional.introspections";
    private static final String METRIC_INTROSPECTION_VALIDITY_CHECKS = METRIC_INTROSPECTION + ".validity.checks";
    private static final String METRIC_INTROSPECTION_REQUESTS = METRIC_INTROSPECTION + ".endpoint.requests";
    private static final String TAG_ISSUER = "issuer";
    private static final String TAG_ACTIVE = "active";
    private static final String TAG_INTROSPECTED = "introspected";
    private static final String VALUE_UNKNOWN = "unknown";

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<MeterRegistry> meterRegistry;

    void recordConditionalTokenIntrospection(Jwt jwt, boolean introspected, Boolean active) {
        meterRegistry.ifPresent(registry ->
                registry.counter(METRIC_INTROSPECTION_CONDITIONAL_INTROSPECTIONS,
                                TAG_ISSUER, getIssuer(jwt),
                                TAG_INTROSPECTED, Boolean.toString(introspected),
                                TAG_ACTIVE, active != null ? active.toString() : VALUE_UNKNOWN)
                                .increment());
    }

    void recordValidityCheck(Jwt jwt, boolean active) {
        meterRegistry.ifPresent(registry ->
                registry.counter(METRIC_INTROSPECTION_VALIDITY_CHECKS,
                                TAG_ISSUER, getIssuer(jwt),
                                TAG_ACTIVE, Boolean.toString(active))
                                .increment());
    }

    JeapTokenIntrospector timeTokenIntrospectionRequests(JeapTokenIntrospector tokenIntrospector, String issuer) {
        if (meterRegistry.isPresent()) {
            return (String token) -> {
                Timer.Sample sample = Timer.start(meterRegistry.get());
                try {
                    Map<String, Object> attributes = tokenIntrospector.introspect(token);
                    stopIntrospectionRequestTimer(sample, issuer, true);
                    return attributes;
                } catch (JeapIntrospectionInvalidTokenException tie) {
                    stopIntrospectionRequestTimer(sample, issuer, false);
                    throw tie;
                } catch (Exception e) {
                    stopIntrospectionRequestTimer(sample, issuer, null);
                    throw e;
                }
            };
        } else {
            return tokenIntrospector;
        }
    }

    private void stopIntrospectionRequestTimer(Timer.Sample sample, String issuer, Boolean active) {
        meterRegistry.ifPresent( registry -> {
            Timer timer = Timer.builder(METRIC_INTROSPECTION_REQUESTS)
                                .tags(TAG_ISSUER, issuer, TAG_ACTIVE, active != null ? active.toString() : VALUE_UNKNOWN)
                                .publishPercentiles(0.5, 0.75, 0.9, 0.99)
                                .register(registry);
            sample.stop(timer);
        });
    }

    private static String getIssuer(Jwt jwt) {
        return jwt.getIssuer() != null ? jwt.getIssuer().toString() : VALUE_UNKNOWN;
    }

}
