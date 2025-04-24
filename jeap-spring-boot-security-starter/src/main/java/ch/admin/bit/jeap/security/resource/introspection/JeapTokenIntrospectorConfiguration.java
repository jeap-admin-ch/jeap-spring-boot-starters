package ch.admin.bit.jeap.security.resource.introspection;

import java.time.Duration;

public record JeapTokenIntrospectorConfiguration(
        String issuer,
        String introspectionUri,
        String clientId,
        String clientSecret,
        Duration connectTimeout,
        Duration readTimeout
) {}
