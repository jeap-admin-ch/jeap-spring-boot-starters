package ch.admin.bit.jeap.security.resource.introspection;

import ch.admin.bit.jeap.security.resource.properties.IntrospectionCacheProperties;
import ch.admin.bit.jeap.security.resource.properties.ResourceServerProperties;
import ch.qos.logback.classic.Level;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static ch.admin.bit.jeap.security.resource.introspection.LightweightTokenIntrospectionCondition.ROLES_PRUNED_CHARS_CLAIM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the token introspection cache as configured by the introspection auto-configuration: introspection responses
 * are cached per issuer for the transparent introspection only, and only for tokens that can be identified.
 */
@SuppressWarnings("SameParameterValue")
@Import({CachingJeapIntrospectionTest.TestConfig.class})
@ActiveProfiles("introspection-cache")
@EnableConfigurationProperties(ResourceServerProperties.class)
@SpringBootTest(classes = CachingJeapIntrospectionTest.MvcJeapIntrospectionConfigurationUnconditional.class)
class CachingJeapIntrospectionTest {

    private static final String ISSUER_CACHED = "https://keycloak/auth/realm/cached";
    private static final String ISSUER_UNCACHED = "https://keycloak/auth/realm/uncached";
    private static final String INTROSPECTION_COUNT_ATTRIBUTE = "introspection-count";
    private static final String USER_ROLES_CLAIM = "userroles";

    private static final String METRIC_CACHE_LOOKUPS = "jeap.security.token.introspection.cache.lookups";
    private static final String TAG_ISSUER = "issuer";
    private static final String TAG_RESULT = "result";
    private static final String INTROSPECTION_LOGGER = "ch.admin.bit.jeap.security.resource.introspection";

    @Autowired
    private JeapJwtIntrospection jwtIntrospection;

    @Autowired
    private JeapTokenIntrospectionCacheFactory cacheFactory;

    @Autowired
    private ResourceServerProperties resourceServerProperties;

    @Autowired
    private CountingJeapTokenIntrospectorFactory introspectorFactory;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void init() {
        meterRegistry.clear();
        introspectorFactory.resetCounts();
    }

    @Test
    void cacheProperties_boundPerAuthorizationServer() {
        IntrospectionCacheProperties cachedIssuerCache = resourceServerProperties.getAuthorizationServer().getIntrospection().getCache();
        assertThat(cachedIssuerCache.isEnabled()).isTrue();
        assertThat(cachedIssuerCache.getMaximumSize()).isEqualTo(100);
        assertThat(cachedIssuerCache.getTimeToLive()).isEqualTo(Duration.ofMinutes(1));

        IntrospectionCacheProperties uncachedIssuerCache = resourceServerProperties.getAuthServers().getFirst().getIntrospection().getCache();
        assertThat(uncachedIssuerCache.isEnabled()).isFalse();
        assertThat(uncachedIssuerCache.getMaximumSize()).isEqualTo(1000);
        assertThat(uncachedIssuerCache.getTimeToLive()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void defaultCacheFactory_isCaffeineBased() {
        assertThat(cacheFactory).isInstanceOf(CaffeineJeapTokenIntrospectionCacheFactory.class);
    }

    @Test
    void introspectIfNeeded_whenCachedIssuerAndSameTokenTwice_thenIntrospectedOnceAndSameAttributes() {
        Jwt jwt = createLightweightJwt(ISSUER_CACHED);

        Jwt first = jwtIntrospection.introspectIfNeeded(jwt);
        Jwt second = jwtIntrospection.introspectIfNeeded(jwt);

        assertThat(introspectorFactory.count(ISSUER_CACHED)).isEqualTo(1);
        assertThat(first.getClaimAsBoolean("active")).isTrue();
        assertThat(first.<Integer>getClaim(INTROSPECTION_COUNT_ATTRIBUTE)).isEqualTo(1);
        assertThat(second.getClaims()).isEqualTo(first.getClaims());
        assertThat(cacheLookups(ISSUER_CACHED, "miss")).isEqualTo(1.0);
        assertThat(cacheLookups(ISSUER_CACHED, "hit")).isEqualTo(1.0);
    }

    @Test
    void introspectIfNeeded_whenUncachedIssuerAndSameTokenTwice_thenIntrospectedTwice() {
        Jwt jwt = createLightweightJwt(ISSUER_UNCACHED);

        jwtIntrospection.introspectIfNeeded(jwt);
        jwtIntrospection.introspectIfNeeded(jwt);

        assertThat(introspectorFactory.count(ISSUER_UNCACHED)).isEqualTo(2);
        assertThat(meterRegistry.find(METRIC_CACHE_LOOKUPS).tag(TAG_ISSUER, ISSUER_UNCACHED).counters()).isEmpty();
    }

    @Test
    void introspectIfNeeded_whenDifferentTokens_thenEachIntrospected() {
        jwtIntrospection.introspectIfNeeded(createLightweightJwt(ISSUER_CACHED));
        jwtIntrospection.introspectIfNeeded(createLightweightJwt(ISSUER_CACHED));

        assertThat(introspectorFactory.count(ISSUER_CACHED)).isEqualTo(2);
        assertThat(cacheLookups(ISSUER_CACHED, "miss")).isEqualTo(2.0);
    }

    @Test
    void isValid_whenCachedIssuer_thenAlwaysIntrospectedAndCachePopulatedWithResult() {
        Jwt jwt = createLightweightJwt(ISSUER_CACHED);

        assertThat(jwtIntrospection.isValid(jwt)).isTrue();
        assertThat(jwtIntrospection.isValid(jwt)).isTrue();
        assertThat(introspectorFactory.count(ISSUER_CACHED)).isEqualTo(2);
        assertThat(meterRegistry.find(METRIC_CACHE_LOOKUPS).counters()).isEmpty();

        // The validity checks populated the cache, the transparent introspection is served from it
        Jwt enrichedJwt = jwtIntrospection.introspectIfNeeded(jwt);
        assertThat(introspectorFactory.count(ISSUER_CACHED)).isEqualTo(2);
        assertThat(enrichedJwt.<Integer>getClaim(INTROSPECTION_COUNT_ATTRIBUTE)).isEqualTo(2);
        assertThat(cacheLookups(ISSUER_CACHED, "hit")).isEqualTo(1.0);
    }

    @Test
    void introspectIfNeeded_whenTokenWithoutTokenId_thenNotCached() {
        Jwt jwt = createLightweightJwtBuilder(ISSUER_CACHED, "token-without-jti-" + UUID.randomUUID()).build();

        jwtIntrospection.introspectIfNeeded(jwt);
        jwtIntrospection.introspectIfNeeded(jwt);

        assertThat(introspectorFactory.count(ISSUER_CACHED)).isEqualTo(2);
        assertThat(cacheLookups(ISSUER_CACHED, "skipped")).isEqualTo(2.0);
    }

    @Test
    void introspectIfNeeded_whenTokenInactive_thenNotCached() {
        Jwt jwt = createLightweightJwt(ISSUER_CACHED);
        introspectorFactory.markInactive(jwt);

        assertThatThrownBy(() -> jwtIntrospection.introspectIfNeeded(jwt)).isInstanceOf(JeapIntrospectionInvalidTokenException.class);
        assertThatThrownBy(() -> jwtIntrospection.introspectIfNeeded(jwt)).isInstanceOf(JeapIntrospectionInvalidTokenException.class);

        assertThat(introspectorFactory.count(ISSUER_CACHED)).isEqualTo(2);
        assertThat(cacheLookups(ISSUER_CACHED, "miss")).isEqualTo(2.0);
    }

    @Test
    void isValid_whenCacheFilledAndTokenBecameInactive_thenBypassesCacheAndReportsInvalid() {
        Jwt jwt = createLightweightJwt(ISSUER_CACHED);
        jwtIntrospection.introspectIfNeeded(jwt); // fills the cache with the active response
        assertThat(introspectorFactory.count(ISSUER_CACHED)).isEqualTo(1);

        introspectorFactory.markInactive(jwt); // e.g. the token has been revoked in the meantime

        assertThat(jwtIntrospection.isValid(jwt)).isFalse();
        assertThat(introspectorFactory.count(ISSUER_CACHED)).isEqualTo(2);
        // The validity check removed the cached response, the transparent introspection has to introspect again
        assertThatThrownBy(() -> jwtIntrospection.introspectIfNeeded(jwt)).isInstanceOf(JeapIntrospectionInvalidTokenException.class);
        assertThat(introspectorFactory.count(ISSUER_CACHED)).isEqualTo(3);
    }

    @Test
    void isValid_whenCacheFilledAndTokenStillActive_thenRefreshesCachedResponse() {
        Jwt jwt = createLightweightJwt(ISSUER_CACHED);
        assertThat(jwtIntrospection.introspectIfNeeded(jwt).<Integer>getClaim(INTROSPECTION_COUNT_ATTRIBUTE)).isEqualTo(1);

        assertThat(jwtIntrospection.isValid(jwt)).isTrue();
        assertThat(introspectorFactory.count(ISSUER_CACHED)).isEqualTo(2);

        // The validity check replaced the cached response with the fresh one
        assertThat(jwtIntrospection.introspectIfNeeded(jwt).<Integer>getClaim(INTROSPECTION_COUNT_ATTRIBUTE)).isEqualTo(2);
        assertThat(introspectorFactory.count(ISSUER_CACHED)).isEqualTo(2);
    }

    @Test
    void isValid_whenCacheFilledAndIntrospectionFails_thenBypassesCacheAndReportsInvalid() {
        Jwt jwt = createLightweightJwt(ISSUER_CACHED);
        jwtIntrospection.introspectIfNeeded(jwt); // fills the cache with the active response
        assertThat(introspectorFactory.count(ISSUER_CACHED)).isEqualTo(1);

        introspectorFactory.markFailing(jwt); // e.g. the authorization server has become unavailable

        assertThat(jwtIntrospection.isValid(jwt)).isFalse();
        assertThat(introspectorFactory.count(ISSUER_CACHED)).isEqualTo(2);
        // The failed validity check left the cached response untouched
        assertThat(jwtIntrospection.introspectIfNeeded(jwt).<Integer>getClaim(INTROSPECTION_COUNT_ATTRIBUTE)).isEqualTo(1);
        assertThat(introspectorFactory.count(ISSUER_CACHED)).isEqualTo(2);
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    void introspectIfNeeded_whenCachedIssuer_thenNestedClaimsOfEnrichedTokenUnmodifiable() {
        Jwt enrichedJwt = jwtIntrospection.introspectIfNeeded(createLightweightJwt(ISSUER_CACHED));

        List<String> userroles = enrichedJwt.getClaim(USER_ROLES_CLAIM);
        assertThat(userroles).containsExactly("reader");
        assertThatThrownBy(() -> userroles.add("admin")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void introspectIfNeeded_whenCachedIssuerAndSameTokenTwice_thenLogsCacheMissThenHit() {
        Jwt jwt = createLightweightJwt(ISSUER_CACHED);
        String token = cachedTokenDescription(jwt);

        try (LogCapture logCapture = LogCapture.of(INTROSPECTION_LOGGER)) {
            jwtIntrospection.introspectIfNeeded(jwt);
            jwtIntrospection.introspectIfNeeded(jwt);

            assertThat(logCapture.messages(Level.TRACE)).containsExactly(
                    "No cached introspection response for token [" + token + "], introspecting it on the introspection endpoint.",
                    "Cached the introspection response for token [" + token + "] for " + lifetimeDescription(jwt) + ".",
                    "Serving the introspection response for token [" + token + "] from the cache.");
        }
    }

    @Test
    void isValid_whenCachedIssuerTwice_thenLogsValidityChecksAndCacheUpdates() {
        Jwt jwt = createLightweightJwt(ISSUER_CACHED);
        String token = cachedTokenDescription(jwt);

        try (LogCapture logCapture = LogCapture.of(INTROSPECTION_LOGGER)) {
            assertThat(jwtIntrospection.isValid(jwt)).isTrue();
            assertThat(jwtIntrospection.isValid(jwt)).isTrue();

            assertThat(logCapture.messages(Level.TRACE)).containsExactly(
                    validityCheckDescription(jwt),
                    "Cached the introspection response for token [" + token + "] for " + lifetimeDescription(jwt) + ".",
                    validityCheckDescription(jwt),
                    "Replaced the cached introspection response for token [" + token + "] by the fresh response, cached for " + lifetimeDescription(jwt) + ".");
        }
    }

    @Test
    void isValid_whenCacheFilledAndTokenBecameInactive_thenLogsRemovalOfCachedResponse() {
        Jwt jwt = createLightweightJwt(ISSUER_CACHED);
        jwtIntrospection.introspectIfNeeded(jwt); // fills the cache with the active response
        introspectorFactory.markInactive(jwt);

        try (LogCapture logCapture = LogCapture.of(INTROSPECTION_LOGGER)) {
            assertThat(jwtIntrospection.isValid(jwt)).isFalse();

            assertThat(logCapture.messages(Level.TRACE)).containsExactly(
                    validityCheckDescription(jwt),
                    "Removed the cached introspection response for token [" + cachedTokenDescription(jwt) + "], as the fresh introspection reported the token inactive.");
        }
    }

    @Test
    void isValid_whenCacheFilledAndIntrospectionFails_thenLogsRetentionOfCachedResponse() {
        Jwt jwt = createLightweightJwt(ISSUER_CACHED);
        jwtIntrospection.introspectIfNeeded(jwt); // fills the cache with the active response
        introspectorFactory.markFailing(jwt);

        try (LogCapture logCapture = LogCapture.of(INTROSPECTION_LOGGER)) {
            assertThat(jwtIntrospection.isValid(jwt)).isFalse();

            assertThat(logCapture.messages(Level.TRACE)).containsExactly(
                    validityCheckDescription(jwt),
                    "Kept the cached introspection response for token [" + cachedTokenDescription(jwt) + "], as the fresh introspection failed.");
        }
    }

    @Test
    void introspectIfNeeded_whenTokenWithoutTokenId_thenLogsIntrospectionWithoutCaching() {
        Jwt jwt = createLightweightJwtBuilder(ISSUER_CACHED, "token-without-jti-" + UUID.randomUUID()).build();

        try (LogCapture logCapture = LogCapture.of(INTROSPECTION_LOGGER)) {
            jwtIntrospection.introspectIfNeeded(jwt);

            assertThat(logCapture.messages(Level.TRACE)).containsExactly(
                    "Introspecting token [" + tokenDescription(jwt) + "] on the introspection endpoint without caching, as the token has no token id (jti).");
        }
    }

    @Test
    void isValid_whenTokenWithoutTokenId_thenLogsValidityCheckWithoutCaching() {
        Jwt jwt = createLightweightJwtBuilder(ISSUER_CACHED, "token-without-jti-" + UUID.randomUUID()).build();

        try (LogCapture logCapture = LogCapture.of(INTROSPECTION_LOGGER)) {
            assertThat(jwtIntrospection.isValid(jwt)).isTrue();

            assertThat(logCapture.messages(Level.TRACE)).containsExactly(
                    "Introspecting token [" + tokenDescription(jwt) + "] on the introspection endpoint for a validity check without caching, as the token has no token id (jti).");
        }
    }

    @Test
    void introspectIfNeeded_whenUncachedIssuer_thenLogsIntrospectionWithoutCache() {
        Jwt jwt = createLightweightJwt(ISSUER_UNCACHED);

        try (LogCapture logCapture = LogCapture.of(INTROSPECTION_LOGGER)) {
            jwtIntrospection.introspectIfNeeded(jwt);

            assertThat(logCapture.messages(Level.TRACE)).containsExactly(
                    "Introspecting token [" + tokenDescription(jwt) + "] on the introspection endpoint, as no introspection cache is configured for its issuer.");
        }
    }

    private double cacheLookups(String issuer, String result) {
        return meterRegistry.counter(METRIC_CACHE_LOOKUPS, TAG_ISSUER, issuer, TAG_RESULT, result).count();
    }

    /**
     * The token as described in the log messages: issuer, subject and, if the token has one, the token id.
     */
    private static String tokenDescription(Jwt jwt) {
        String description = "issuer='" + jwt.getIssuer() + "', subject='" + jwt.getSubject() + "'";
        return jwt.getId() != null ? description + ", jti='" + jwt.getId() + "'" : description;
    }

    /**
     * The token as described in the log messages about its cache entry: additionally a prefix of the token hash.
     */
    private static String cachedTokenDescription(Jwt jwt) {
        JeapTokenIntrospectionCacheKey key = JeapTokenIntrospectionCacheKey.of(jwt).orElseThrow();
        return tokenDescription(jwt) + ", hash='" + key.tokenHash().substring(0, 8) + "'";
    }

    /**
     * The log message of a validity check of the token on the introspection endpoint.
     */
    private static String validityCheckDescription(Jwt jwt) {
        return "Introspecting token [" + cachedTokenDescription(jwt) + "] on the introspection endpoint for a validity check, bringing the cache up to date with the result.";
    }

    /**
     * The lifetime of a cache entry as described in the log messages: the cached issuer's time to live of one minute
     * is shorter than the remaining lifetime of the token, and the test introspector's responses declare no expiry.
     */
    private static String lifetimeDescription(Jwt jwt) {
        return "PT1M (time to live PT1M, token expires at " + jwt.getExpiresAt() + ", response declares no expiry)";
    }

    private static Jwt createLightweightJwt(String issuer) {
        String tokenId = UUID.randomUUID().toString();
        return createLightweightJwtBuilder(issuer, "token-" + tokenId).jti(tokenId).build();
    }

    private static Jwt.Builder createLightweightJwtBuilder(String issuer, String tokenValue) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .issuer(issuer)
                .subject("1234567890")
                .expiresAt(Instant.now().plus(Duration.ofMinutes(5)))
                .claim(ROLES_PRUNED_CHARS_CLAIM, 10000);
    }

    @Configuration
    // We need to get rid of the "@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)" on
    // MvcJeapIntrospectionConfiguration to be able to activate the configuration class in this test. As this project
    // provides the WebMVC dependencies, the conditional-on-servlet-webapp would not be satisfied otherwise.
    static class MvcJeapIntrospectionConfigurationUnconditional extends MvcJeapIntrospectionConfiguration {}

    @TestConfiguration
    static class TestConfig {

        @Bean
        CountingJeapTokenIntrospectorFactory jeapTokenIntrospectorFactory() {
            return new CountingJeapTokenIntrospectorFactory();
        }

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

    }

    /**
     * Counts the introspection requests per issuer. Tokens are reported active, unless they have been marked inactive
     * or failing. Responses contain a mutable roles list to check that cached responses are protected from modification.
     */
    static class CountingJeapTokenIntrospectorFactory implements JeapTokenIntrospectorFactory {

        private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();
        private final Set<String> inactiveTokens = ConcurrentHashMap.newKeySet();
        private final Set<String> failingTokens = ConcurrentHashMap.newKeySet();

        @Override
        public JeapTokenIntrospector create(JeapTokenIntrospectorConfiguration config) {
            AtomicInteger count = counts.computeIfAbsent(config.issuer(), _ -> new AtomicInteger());
            return token -> {
                int introspectionCount = count.incrementAndGet();
                if (inactiveTokens.contains(token)) {
                    throw new JeapIntrospectionInvalidTokenException();
                }
                if (failingTokens.contains(token)) {
                    throw new JeapIntrospectionException("Introspection endpoint not available");
                }
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("active", true);
                attributes.put(INTROSPECTION_COUNT_ATTRIBUTE, introspectionCount);
                attributes.put(USER_ROLES_CLAIM, new ArrayList<>(List.of("reader")));
                return attributes;
            };
        }

        void markInactive(Jwt jwt) {
            inactiveTokens.add(jwt.getTokenValue());
        }

        void markFailing(Jwt jwt) {
            failingTokens.add(jwt.getTokenValue());
        }

        int count(String issuer) {
            return counts.getOrDefault(issuer, new AtomicInteger()).get();
        }

        void resetCounts() {
            counts.values().forEach(count -> count.set(0));
        }

    }

}
