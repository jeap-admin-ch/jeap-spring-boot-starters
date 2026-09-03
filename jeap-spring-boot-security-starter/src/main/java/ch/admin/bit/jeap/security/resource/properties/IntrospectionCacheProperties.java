package ch.admin.bit.jeap.security.resource.properties;

import lombok.Data;

import java.time.Duration;

/**
 * Configuration of the local cache for the token introspection responses of an authorization server.
 */
@Data
public class IntrospectionCacheProperties {

    /**
     * Whether the token introspection responses of this authorization server are cached locally in the instance.
     * Caching only applies to the transparent introspection of tokens that enriches them with additional data.
     * Explicit validity checks always query the introspection endpoint. Disabled by default.
     */
    private boolean enabled = false;

    /**
     * Maximum number of introspection responses kept in the cache.
     */
    private long maximumSize = 1000;

    /**
     * Maximum time a cached introspection response is kept. A cached response additionally expires with the token it
     * belongs to.
     */
    private Duration timeToLive = Duration.ofMinutes(5);

    /**
     * Validate these cache properties. Does nothing if the cache is disabled.
     *
     * @param issuerUri The issuer uri of the authorization server these cache properties belong to.
     */
    public void validate(String issuerUri) {
        if (!enabled) {
            return;
        }
        if (maximumSize <= 0) {
            throw new IllegalArgumentException(issuerUri + ": introspection cache maximum-size must be greater than 0 if the cache is enabled.");
        }
        if (timeToLive == null || timeToLive.isZero() || timeToLive.isNegative()) {
            throw new IllegalArgumentException(issuerUri + ": introspection cache time-to-live must be a positive duration if the cache is enabled.");
        }
    }

}
