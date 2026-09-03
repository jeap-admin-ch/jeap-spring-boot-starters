package ch.admin.bit.jeap.security.resource.introspection;

import ch.admin.bit.jeap.security.resource.introspection.JeapTokenIntrospectionMetrics.CacheLookupResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Introspecting a JSON Web Token (non-opaque) using the token introspector configured for a token's issuer, optionally
 * serving the introspection response from the token introspection cache configured for the issuer. Every introspection
 * is logged on level TRACE, telling whether the response has been served from the cache or the introspection endpoint
 * has been queried, and why.
 */
@Slf4j
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
class JeapJwtIntrospector {

    private final Map<String, IssuerIntrospection> issuerIntrospections = new HashMap<>();
    private final Set<String> issuersWarnedAboutUncacheableTokens = ConcurrentHashMap.newKeySet();
    private final Optional<JeapTokenIntrospectionMetrics> jeapTokenIntrospectionMetrics;

    /**
     * Construct a JeapJwtIntrospector instance from the given issuer to introspection mappings.
     *
     * @param issuerIntrospections          The issuer to introspection mappings
     * @param jeapTokenIntrospectionMetrics The introspection metrics, if present
     */
    JeapJwtIntrospector(Map<String, IssuerIntrospection> issuerIntrospections, Optional<JeapTokenIntrospectionMetrics> jeapTokenIntrospectionMetrics) {
        this.issuerIntrospections.putAll(issuerIntrospections);
        this.jeapTokenIntrospectionMetrics = jeapTokenIntrospectionMetrics;
    }

    /**
     * Introspects a JWT using the token introspector configured for the token's issuer. The introspection response is
     * served from the token introspection cache if the issuer has one configured and the token can be cached.
     *
     * @param jwt The JSON Web Token to introspect
     * @return A map of attributes returned by the introspector
     * @throws JeapIntrospectionInvalidTokenException if the token is not valid
     * @throws JeapIntrospectionException if the introspection failed
     */
    Map<String, Object> introspect(Jwt jwt) throws JeapIntrospectionException, JeapIntrospectionInvalidTokenException {
        return introspect(jwt, true);
    }

    /**
     * Introspects a JWT using the token introspector configured for the token's issuer, always querying the introspection
     * endpoint, i.e. the result is never served from the token introspection cache. To be used when the introspection is
     * meant to check the current validity of the token, which a cached response cannot answer. If the issuer has a token
     * introspection cache configured, the cache is brought up to date with the result as a side effect.
     *
     * @param jwt The JSON Web Token to introspect
     * @return A map of attributes returned by the introspector
     * @throws JeapIntrospectionInvalidTokenException if the token is not valid
     * @throws JeapIntrospectionException if the introspection failed
     */
    @SuppressWarnings("UnusedReturnValue")
    Map<String, Object> introspectFresh(Jwt jwt) throws JeapIntrospectionException, JeapIntrospectionInvalidTokenException {
        return introspect(jwt, false);
    }

    private Map<String, Object> introspect(Jwt jwt, boolean cacheLookupAllowed) {
        try {
            IssuerIntrospection issuerIntrospection = getIssuerIntrospection(jwt);
            if (!issuerIntrospection.isCacheEnabled()) {
                log.trace("Introspecting token [{}] on the introspection endpoint, as no introspection cache is configured for its issuer.",
                        TokenDescription.of(jwt));
                return issuerIntrospection.tokenIntrospector().introspect(jwt.getTokenValue());
            }
            return cacheLookupAllowed ? introspectCached(jwt, issuerIntrospection) : introspectFreshUpdatingCache(jwt, issuerIntrospection);
        } catch (JeapIntrospectionException jie) {
            log.error("Introspection failed for a token from issuer '{}' for subject '{}'.", jwt.getIssuer(), jwt.getSubject(), jie);
            throw jie; // rethrow to keep the exact exception (sub) type
        } catch (Exception e) {
            String msg = "An error occurred while introspecting a token from issuer '%s' for subject '%s'.".formatted(jwt.getIssuer(), jwt.getSubject());
            log.error(msg, e);
            throw new JeapIntrospectionException(msg, e);
        }
    }

    private Map<String, Object> introspectCached(Jwt jwt, IssuerIntrospection issuerIntrospection) {
        JeapTokenIntrospector tokenIntrospector = issuerIntrospection.tokenIntrospector();
        Optional<JeapTokenIntrospectionCacheKey> cacheKey = cacheKeyOf(jwt);
        if (cacheKey.isEmpty()) {
            log.trace("Introspecting token [{}] on the introspection endpoint without caching, as the token has no token id (jti).",
                    TokenDescription.of(jwt));
            recordCacheLookup(jwt, CacheLookupResult.SKIPPED);
            return tokenIntrospector.introspect(jwt.getTokenValue());
        }
        TokenDescription token = TokenDescription.of(jwt, cacheKey.get());
        AtomicBoolean introspected = new AtomicBoolean(false);
        Map<String, Object> attributes;
        try {
            attributes = issuerIntrospection.cache().getOrIntrospect(cacheKey.get(), jwt, () -> {
                introspected.set(true);
                log.trace("No cached introspection response for token [{}], introspecting it on the introspection endpoint.", token);
                return tokenIntrospector.introspect(jwt.getTokenValue());
            });
        } finally {
            recordCacheLookup(jwt, introspected.get() ? CacheLookupResult.MISS : CacheLookupResult.HIT);
        }
        if (!introspected.get()) {
            log.trace("Serving the introspection response for token [{}] from the cache.", token);
        }
        return attributes;
    }

    private Map<String, Object> introspectFreshUpdatingCache(Jwt jwt, IssuerIntrospection issuerIntrospection) {
        JeapTokenIntrospector tokenIntrospector = issuerIntrospection.tokenIntrospector();
        Optional<JeapTokenIntrospectionCacheKey> cacheKey = cacheKeyOf(jwt);
        if (cacheKey.isEmpty()) {
            log.trace("Introspecting token [{}] on the introspection endpoint for a validity check without caching, as the token has no token id (jti).",
                    TokenDescription.of(jwt));
            return tokenIntrospector.introspect(jwt.getTokenValue());
        }
        log.trace("Introspecting token [{}] on the introspection endpoint for a validity check, bringing the cache up to date with the result.",
                TokenDescription.of(jwt, cacheKey.get()));
        return issuerIntrospection.cache().introspect(cacheKey.get(), jwt, () -> tokenIntrospector.introspect(jwt.getTokenValue()));
    }

    private Optional<JeapTokenIntrospectionCacheKey> cacheKeyOf(Jwt jwt) {
        Optional<JeapTokenIntrospectionCacheKey> cacheKey = JeapTokenIntrospectionCacheKey.of(jwt);
        if (cacheKey.isEmpty()) {
            warnAboutUncacheableToken(jwt);
        }
        return cacheKey;
    }

    private void warnAboutUncacheableToken(Jwt jwt) {
        String issuer = String.valueOf(jwt.getIssuer());
        if (issuersWarnedAboutUncacheableTokens.add(issuer)) {
            log.warn("Not caching the introspection responses for tokens from issuer '{}' that lack a token id (jti claim), " +
                    "as the token id safeguards the cache key against hash collisions between different tokens. Such tokens " +
                    "are always introspected on the introspection endpoint. This warning is logged only once per issuer.", issuer);
        }
    }

    private void recordCacheLookup(Jwt jwt, CacheLookupResult result) {
        jeapTokenIntrospectionMetrics.ifPresent(metrics -> metrics.recordCacheLookup(jwt, result));
    }

    private IssuerIntrospection getIssuerIntrospection(Jwt jwt) {
        if (jwt.getIssuer() == null) {
            throw new JeapIntrospectionUnknownIssuerException("null", "The token has no issuer claim (iss).");
        }
        final String issuer = jwt.getIssuer().toString();
        IssuerIntrospection issuerIntrospection = issuerIntrospections.get(issuer);
        if (issuerIntrospection == null) {
            throw new JeapIntrospectionUnknownIssuerException(issuer, "No token introspector configured for the issuer.");
        }
        return issuerIntrospection;
    }

    /**
     * The token introspector of an issuer and, if caching is enabled for the issuer, its token introspection cache.
     *
     * @param tokenIntrospector The token introspector querying the introspection endpoint of the issuer
     * @param cache             The token introspection cache of the issuer, {@code null} if caching is not enabled
     */
    record IssuerIntrospection(JeapTokenIntrospector tokenIntrospector, JeapTokenIntrospectionCache cache) {

        static IssuerIntrospection uncached(JeapTokenIntrospector tokenIntrospector) {
            return new IssuerIntrospection(tokenIntrospector, null);
        }

        static IssuerIntrospection cached(JeapTokenIntrospector tokenIntrospector, JeapTokenIntrospectionCache cache) {
            return new IssuerIntrospection(tokenIntrospector, cache);
        }

        boolean isCacheEnabled() {
            return cache != null;
        }

    }

}
