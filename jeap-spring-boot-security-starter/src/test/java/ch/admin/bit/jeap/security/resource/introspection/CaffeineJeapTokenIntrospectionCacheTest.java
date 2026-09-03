package ch.admin.bit.jeap.security.resource.introspection;

import ch.qos.logback.classic.Level;
import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("resource")
class CaffeineJeapTokenIntrospectionCacheTest {

    private static final String ISSUER = "https://keycloak/auth/realm";
    private static final Instant NOW = Instant.parse("2026-09-03T10:00:00Z");
    private static final Duration TIME_TO_LIVE = Duration.ofMinutes(5);
    private static final Duration ONE_SECOND = Duration.ofSeconds(1);
    private static final Map<String, Object> ATTRIBUTES = Map.of("active", true, "sub", "subject");
    private static final String UNBOUNDED_LIFETIME = "PT5M (time to live PT5M, token declares no expiry, response declares no expiry)";

    private final FakeTicker ticker = new FakeTicker();
    private final Clock clock = ticker.clock(NOW);
    private CaffeineJeapTokenIntrospectionCache cache;

    @BeforeEach
    void setUp() {
        cache = createCache(100);
    }

    @Test
    void getOrIntrospect_whenSameTokenLookedUpTwice_thenIntrospectedOnce() {
        Jwt jwt = createJwt("token-1", NOW.plus(TIME_TO_LIVE.multipliedBy(2)));
        CountingIntrospection introspection = new CountingIntrospection();

        Map<String, Object> first = cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        Map<String, Object> second = cache.getOrIntrospect(keyOf(jwt), jwt, introspection);

        assertThat(first).containsAllEntriesOf(ATTRIBUTES);
        assertThat(second).isEqualTo(first);
        assertThat(introspection.count()).isEqualTo(1);
    }

    @Test
    void getOrIntrospect_whenDifferentTokens_thenEachIntrospected() {
        Jwt jwt = createJwt("token-1", null);
        Jwt otherJwt = createJwt("token-2", null);
        CountingIntrospection introspection = new CountingIntrospection();

        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        cache.getOrIntrospect(keyOf(otherJwt), otherJwt, introspection);

        assertThat(introspection.count()).isEqualTo(2);
        assertThat(cache.estimatedSize()).isEqualTo(2);
    }

    @Test
    void getOrIntrospect_whenIntrospectionThrows_thenExceptionPropagatedAndNothingCached() {
        Jwt jwt = createJwt("token-1", null);
        JeapTokenIntrospectionCacheKey key = keyOf(jwt);
        Supplier<Map<String, Object>> failingIntrospection = () -> {
            throw new JeapIntrospectionInvalidTokenException();
        };

        assertThatThrownBy(() -> cache.getOrIntrospect(key, jwt, failingIntrospection))
                .isInstanceOf(JeapIntrospectionInvalidTokenException.class);
        assertThat(cache.estimatedSize()).isZero();

        CountingIntrospection introspection = new CountingIntrospection();
        cache.getOrIntrospect(key, jwt, introspection);
        assertThat(introspection.count()).isEqualTo(1);
    }

    @Test
    void getOrIntrospect_whenTimeToLiveNotYetElapsed_thenServedFromCache() {
        Jwt jwt = createJwt("token-1", null);
        CountingIntrospection introspection = new CountingIntrospection();

        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        ticker.advance(TIME_TO_LIVE.minus(ONE_SECOND));
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);

        assertThat(introspection.count()).isEqualTo(1);
    }

    @Test
    void getOrIntrospect_whenTimeToLiveElapsed_thenIntrospectedAgain() {
        Jwt jwt = createJwt("token-1", null);
        CountingIntrospection introspection = new CountingIntrospection();

        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        ticker.advance(TIME_TO_LIVE.plus(ONE_SECOND));
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);

        assertThat(introspection.count()).isEqualTo(2);
    }

    @Test
    void getOrIntrospect_whenTokenExpiresBeforeTimeToLive_thenEntryExpiresWithToken() {
        Duration remainingTokenLifetime = Duration.ofMinutes(1);
        Jwt jwt = createJwt("token-1", NOW.plus(remainingTokenLifetime));
        CountingIntrospection introspection = new CountingIntrospection();

        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        ticker.advance(remainingTokenLifetime.minus(ONE_SECOND));
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        assertThat(introspection.count()).isEqualTo(1);

        ticker.advance(ONE_SECOND.multipliedBy(2));
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        assertThat(introspection.count()).isEqualTo(2);
    }

    @Test
    void getOrIntrospect_whenTokenExpiresAfterTimeToLive_thenEntryExpiresAfterTimeToLive() {
        Jwt jwt = createJwt("token-1", NOW.plus(Duration.ofHours(1)));
        CountingIntrospection introspection = new CountingIntrospection();

        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        ticker.advance(TIME_TO_LIVE.plus(ONE_SECOND));
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);

        assertThat(introspection.count()).isEqualTo(2);
    }

    @Test
    void getOrIntrospect_whenTokenAlreadyExpired_thenNotServedFromCache() {
        Jwt jwt = createJwt("token-1", NOW.minus(ONE_SECOND));
        CountingIntrospection introspection = new CountingIntrospection();

        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);

        assertThat(introspection.count()).isEqualTo(2);
    }

    @Test
    void getOrIntrospect_whenMaximumSizeExceeded_thenEntriesEvicted() {
        cache = createCache(2);
        CountingIntrospection introspection = new CountingIntrospection();

        for (int i = 0; i < 5; i++) {
            Jwt jwt = createJwt("token-" + i, null);
            cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        }

        assertThat(cache.estimatedSize()).isLessThanOrEqualTo(2);
    }

    @Test
    void getOrIntrospect_thenReturnsUnmodifiableCopyOfIntrospectionResponse() {
        Jwt jwt = createJwt("token-1", null);
        Map<String, Object> response = new HashMap<>(ATTRIBUTES);

        Map<String, Object> cached = cache.getOrIntrospect(keyOf(jwt), jwt, () -> response);
        response.put("added-later", "value");

        assertThatThrownBy(() -> cached.put("key", "value")).isInstanceOf(UnsupportedOperationException.class);
        assertThat(cache.getOrIntrospect(keyOf(jwt), jwt, () -> response)).doesNotContainKey("added-later");
    }

    @Test
    void getOrIntrospect_thenNestedClaimsUnmodifiableAndDecoupledFromResponse() {
        Jwt jwt = createJwt("token-1", null);
        List<String> userroles = new ArrayList<>(List.of("reader"));
        Map<String, List<String>> bproles = new HashMap<>(Map.of("bp1", new ArrayList<>(List.of("bp-reader"))));
        Map<String, Object> response = new HashMap<>(Map.of("active", true, "userroles", userroles, "bproles", bproles));

        Map<String, Object> cached = cache.getOrIntrospect(keyOf(jwt), jwt, () -> response);
        userroles.add("admin");
        bproles.get("bp1").add("bp-admin");

        @SuppressWarnings("unchecked")
        List<String> cachedUserroles = (List<String>) cached.get("userroles");
        @SuppressWarnings("unchecked")
        Map<String, List<String>> cachedBproles = (Map<String, List<String>>) cached.get("bproles");
        assertThat(cachedUserroles).containsExactly("reader");
        assertThat(cachedBproles.get("bp1")).containsExactly("bp-reader");
        assertThatThrownBy(() -> cachedUserroles.add("admin")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> cachedBproles.put("bp2", List.of())).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> cachedBproles.get("bp1").add("bp-admin")).isInstanceOf(UnsupportedOperationException.class);
        // The next lookup returns the unmodified cached response
        assertThat(cache.getOrIntrospect(keyOf(jwt), jwt, () -> response)).isSameAs(cached);
    }

    @Test
    void getOrIntrospect_whenResponseExpiresBeforeTokenAndTimeToLive_thenEntryExpiresWithResponse() {
        Duration remainingResponseLifetime = Duration.ofMinutes(1);
        Jwt jwt = createJwt("token-1", NOW.plus(Duration.ofMinutes(10)));
        CountingIntrospection introspection = new CountingIntrospection(Map.of("active", true, "exp", NOW.plus(remainingResponseLifetime)));

        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        ticker.advance(remainingResponseLifetime.minus(ONE_SECOND));
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        assertThat(introspection.count()).isEqualTo(1);

        ticker.advance(ONE_SECOND.multipliedBy(2));
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        assertThat(introspection.count()).isEqualTo(2);
    }

    @Test
    void getOrIntrospect_whenResponseExpiryGivenAsEpochSeconds_thenEntryExpiresWithResponse() {
        Duration remainingResponseLifetime = Duration.ofMinutes(1);
        Jwt jwt = createJwt("token-1", null);
        CountingIntrospection introspection = new CountingIntrospection(Map.of("active", true, "exp", NOW.plus(remainingResponseLifetime).getEpochSecond()));

        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        ticker.advance(remainingResponseLifetime.plus(ONE_SECOND));
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);

        assertThat(introspection.count()).isEqualTo(2);
    }

    @Test
    void getOrIntrospect_whenResponseExpiresAfterTokenAndTimeToLive_thenEntryExpiresAfterTimeToLive() {
        Jwt jwt = createJwt("token-1", NOW.plus(Duration.ofHours(1)));
        CountingIntrospection introspection = new CountingIntrospection(Map.of("active", true, "exp", NOW.plus(Duration.ofHours(2))));

        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        ticker.advance(TIME_TO_LIVE.minus(ONE_SECOND));
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        assertThat(introspection.count()).isEqualTo(1);

        ticker.advance(ONE_SECOND.multipliedBy(2));
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        assertThat(introspection.count()).isEqualTo(2);
    }

    @Test
    void introspect_whenTokenActive_thenReplacesCachedResponse() {
        Jwt jwt = createJwt("token-1", null);
        CountingIntrospection initialIntrospection = new CountingIntrospection(Map.of("active", true, "version", 1));
        CountingIntrospection freshIntrospection = new CountingIntrospection(Map.of("active", true, "version", 2));
        cache.getOrIntrospect(keyOf(jwt), jwt, initialIntrospection);

        Map<String, Object> fresh = cache.introspect(keyOf(jwt), jwt, freshIntrospection);

        assertThat(fresh).containsEntry("version", 2);
        assertThat(freshIntrospection.count()).isEqualTo(1);
        assertThat(cache.getOrIntrospect(keyOf(jwt), jwt, initialIntrospection)).containsEntry("version", 2);
        assertThat(initialIntrospection.count()).isEqualTo(1);
    }

    @Test
    void introspect_whenTokenActive_thenCachedResponseGetsFreshLifetime() {
        Jwt jwt = createJwt("token-1", null);
        CountingIntrospection introspection = new CountingIntrospection();
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);

        ticker.advance(TIME_TO_LIVE.minus(ONE_SECOND));
        cache.introspect(keyOf(jwt), jwt, introspection);
        assertThat(introspection.count()).isEqualTo(2);

        // The refreshed entry lives on for the full time to live again
        ticker.advance(TIME_TO_LIVE.minus(ONE_SECOND));
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        assertThat(introspection.count()).isEqualTo(2);
        ticker.advance(ONE_SECOND.multipliedBy(2));
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        assertThat(introspection.count()).isEqualTo(3);
    }

    @Test
    void introspect_whenNoCachedResponse_thenCachesFreshResponse() {
        Jwt jwt = createJwt("token-1", null);
        CountingIntrospection introspection = new CountingIntrospection();

        cache.introspect(keyOf(jwt), jwt, introspection);
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);

        assertThat(introspection.count()).isEqualTo(1);
    }

    @Test
    void introspect_whenTokenInactive_thenRemovesCachedResponseAndPropagates() {
        Jwt jwt = createJwt("token-1", null);
        JeapTokenIntrospectionCacheKey key = keyOf(jwt);
        CountingIntrospection introspection = new CountingIntrospection();
        cache.getOrIntrospect(key, jwt, introspection);
        Supplier<Map<String, Object>> inactiveIntrospection = () -> {
            throw new JeapIntrospectionInvalidTokenException();
        };

        assertThatThrownBy(() -> cache.introspect(key, jwt, inactiveIntrospection))
                .isInstanceOf(JeapIntrospectionInvalidTokenException.class);

        assertThat(cache.estimatedSize()).isZero();
        cache.getOrIntrospect(key, jwt, introspection);
        assertThat(introspection.count()).isEqualTo(2);
    }

    @Test
    void introspect_whenIntrospectionFails_thenKeepsCachedResponseAndPropagates() {
        Jwt jwt = createJwt("token-1", null);
        JeapTokenIntrospectionCacheKey key = keyOf(jwt);
        CountingIntrospection introspection = new CountingIntrospection();
        cache.getOrIntrospect(key, jwt, introspection);
        Supplier<Map<String, Object>> failingIntrospection = () -> {
            throw new JeapIntrospectionException("Introspection endpoint not available");
        };

        assertThatThrownBy(() -> cache.introspect(key, jwt, failingIntrospection))
                .isInstanceOf(JeapIntrospectionException.class);

        assertThat(cache.estimatedSize()).isEqualTo(1);
        assertThat(cache.getOrIntrospect(key, jwt, introspection)).containsAllEntriesOf(ATTRIBUTES);
        assertThat(introspection.count()).isEqualTo(1);
    }

    @Test
    void introspect_whenOlderLookupInFlight_thenWaitsForItAndNewerResultWins() throws Exception {
        Jwt jwt = createJwt("token-1", null);
        JeapTokenIntrospectionCacheKey key = keyOf(jwt);
        List<String> events = new CopyOnWriteArrayList<>();
        CountDownLatch olderIntrospectionStarted = new CountDownLatch(1);
        CountDownLatch olderIntrospectionReleased = new CountDownLatch(1);
        CountDownLatch newerCheckArrived = new CountDownLatch(1);
        AtomicReference<RuntimeException> newerCheckFailure = new AtomicReference<>();
        // The older introspection (a lookup) reports the token active, the newer one (a validity check) inactive
        Supplier<Map<String, Object>> olderIntrospection = () -> {
            events.add("older started");
            olderIntrospectionStarted.countDown();
            awaitRelease(olderIntrospectionReleased);
            events.add("older finished");
            return ATTRIBUTES;
        };
        Supplier<Map<String, Object>> newerIntrospection = () -> {
            events.add("newer started");
            throw new JeapIntrospectionInvalidTokenException();
        };
        Thread olderLookup = new Thread(() -> cache.getOrIntrospect(key, jwt, olderIntrospection), "older-lookup");
        Thread newerCheck = new Thread(() -> {
            newerCheckArrived.countDown();
            try {
                cache.introspect(key, jwt, newerIntrospection);
            } catch (RuntimeException e) {
                newerCheckFailure.set(e);
            }
        }, "newer-check");

        olderLookup.start();
        assertThat(olderIntrospectionStarted.await(5, TimeUnit.SECONDS)).isTrue();
        newerCheck.start();
        assertThat(newerCheckArrived.await(5, TimeUnit.SECONDS)).isTrue();
        // While the older introspection is in flight, the newer check must block on the cache without introspecting
        await().atMost(Duration.ofSeconds(5)).until(() -> isBlocked(newerCheck));
        assertThat(events).containsExactly("older started");

        olderIntrospectionReleased.countDown();
        olderLookup.join(Duration.ofSeconds(5).toMillis());
        newerCheck.join(Duration.ofSeconds(5).toMillis());
        assertThat(olderLookup.isAlive()).isFalse();
        assertThat(newerCheck.isAlive()).isFalse();

        // The newer check ran after the older introspection had finished, and its result (the removal) is the final state
        assertThat(events).containsExactly("older started", "older finished", "newer started");
        assertThat(newerCheckFailure.get()).isInstanceOf(JeapIntrospectionInvalidTokenException.class);
        assertThat(cache.estimatedSize()).isZero();
    }

    @Test
    void introspect_whenConcurrentChecksOfSameToken_thenEachIntrospected() throws Exception {
        Jwt jwt = createJwt("token-1", null);
        int checks = 5;
        CountDownLatch checksReleased = new CountDownLatch(1);
        CountingIntrospection introspection = new CountingIntrospection();
        ExecutorService executor = Executors.newFixedThreadPool(checks);
        try {
            List<Future<Map<String, Object>>> results = IntStream.range(0, checks)
                    .mapToObj(_ -> executor.submit(() -> {
                        awaitRelease(checksReleased); // let the checks start together
                        return cache.introspect(keyOf(jwt), jwt, introspection);
                    }))
                    .toList();
            checksReleased.countDown();
            for (Future<Map<String, Object>> result : results) {
                assertThat(result.get(5, TimeUnit.SECONDS)).containsAllEntriesOf(ATTRIBUTES);
            }
        } finally {
            executor.shutdownNow();
        }

        // Unlike lookups, validity checks are never coalesced: every check queries the introspection endpoint itself
        assertThat(introspection.count()).isEqualTo(checks);
        assertThat(cache.estimatedSize()).isEqualTo(1);
    }

    @Test
    void introspect_whenIntrospectionFails_thenCachedResponseKeepsItsOriginalExpiry() {
        Jwt jwt = createJwt("token-1", null);
        JeapTokenIntrospectionCacheKey key = keyOf(jwt);
        CountingIntrospection introspection = new CountingIntrospection();
        cache.getOrIntrospect(key, jwt, introspection);
        Supplier<Map<String, Object>> failingIntrospection = () -> {
            throw new JeapIntrospectionException("Introspection endpoint not available");
        };

        Duration ageAtFailedRefresh = Duration.ofMinutes(4);
        ticker.advance(ageAtFailedRefresh);
        assertThatThrownBy(() -> cache.introspect(key, jwt, failingIntrospection)).isInstanceOf(JeapIntrospectionException.class);

        // The entry still expires at the end of its original time to live, the failed refresh did not restart it
        ticker.advance(TIME_TO_LIVE.minus(ageAtFailedRefresh).minus(ONE_SECOND));
        cache.getOrIntrospect(key, jwt, introspection);
        assertThat(introspection.count()).isEqualTo(1);
        ticker.advance(ONE_SECOND.multipliedBy(2));
        cache.getOrIntrospect(key, jwt, introspection);
        assertThat(introspection.count()).isEqualTo(2);
    }

    @Test
    void introspect_whenTokenActive_thenFreshLifetimeStillBoundedByTokenExpiry() {
        Duration tokenLifetime = Duration.ofMinutes(6);
        Jwt jwt = createJwt("token-1", NOW.plus(tokenLifetime));
        CountingIntrospection introspection = new CountingIntrospection();
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);

        Duration ageAtRefresh = Duration.ofMinutes(4);
        ticker.advance(ageAtRefresh);
        cache.introspect(keyOf(jwt), jwt, introspection);
        assertThat(introspection.count()).isEqualTo(2);

        // The refreshed entry expires with the token, before the restarted time to live would elapse
        ticker.advance(tokenLifetime.minus(ageAtRefresh).minus(ONE_SECOND));
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        assertThat(introspection.count()).isEqualTo(2);
        ticker.advance(ONE_SECOND.multipliedBy(2));
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
        assertThat(introspection.count()).isEqualTo(3);
    }

    @Test
    void introspect_whenTokenActive_thenFreshLifetimeStillBoundedByResponseExpiry() {
        Duration responseLifetime = Duration.ofMinutes(6);
        Jwt jwt = createJwt("token-1", null);
        CountingIntrospection initialIntrospection = new CountingIntrospection();
        CountingIntrospection freshIntrospection = new CountingIntrospection(Map.of("active", true, "exp", NOW.plus(responseLifetime)));
        cache.getOrIntrospect(keyOf(jwt), jwt, initialIntrospection);

        Duration ageAtRefresh = Duration.ofMinutes(4);
        ticker.advance(ageAtRefresh);
        cache.introspect(keyOf(jwt), jwt, freshIntrospection);

        // The refreshed entry expires with the fresh response, before the restarted time to live would elapse
        ticker.advance(responseLifetime.minus(ageAtRefresh).minus(ONE_SECOND));
        cache.getOrIntrospect(keyOf(jwt), jwt, initialIntrospection);
        assertThat(initialIntrospection.count()).isEqualTo(1);
        ticker.advance(ONE_SECOND.multipliedBy(2));
        cache.getOrIntrospect(keyOf(jwt), jwt, initialIntrospection);
        assertThat(initialIntrospection.count()).isEqualTo(2);
    }

    @Test
    void getOrIntrospect_whenConcurrentLookupsOfSameToken_thenIntrospectedOnce() throws Exception {
        Jwt jwt = createJwt("token-1", null);
        CountDownLatch introspectionStarted = new CountDownLatch(1);
        CountDownLatch introspectionReleased = new CountDownLatch(1);
        CountingIntrospection introspection = new CountingIntrospection(() -> {
            introspectionStarted.countDown();
            awaitRelease(introspectionReleased);
        });
        ExecutorService executor = Executors.newFixedThreadPool(5);
        try {
            Future<Map<String, Object>> firstLookup = executor.submit(() -> cache.getOrIntrospect(keyOf(jwt), jwt, introspection));
            assertThat(introspectionStarted.await(5, TimeUnit.SECONDS)).isTrue();
            // The first lookup is blocked in the introspection now, the concurrent lookups must wait for its result
            List<Future<Map<String, Object>>> concurrentLookups = IntStream.range(0, 4)
                    .mapToObj(_ -> executor.submit(() -> cache.getOrIntrospect(keyOf(jwt), jwt, introspection)))
                    .toList();
            introspectionReleased.countDown();
            assertThat(firstLookup.get(5, TimeUnit.SECONDS)).containsAllEntriesOf(ATTRIBUTES);
            for (Future<Map<String, Object>> lookup : concurrentLookups) {
                assertThat(lookup.get(5, TimeUnit.SECONDS)).containsAllEntriesOf(ATTRIBUTES);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(introspection.count()).isEqualTo(1);
    }

    @Test
    void getOrIntrospect_whenResponseCached_thenLogsLifetimeAndItsBounds() {
        Jwt jwt = createJwt("token-1", NOW.plus(Duration.ofMinutes(1)));
        CountingIntrospection introspection = new CountingIntrospection(Map.of("active", true, "exp", NOW.plus(Duration.ofSeconds(30))));

        try (LogCapture logCapture = LogCapture.of(CaffeineJeapTokenIntrospectionCache.class)) {
            cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
            cache.getOrIntrospect(keyOf(jwt), jwt, introspection);

            // Only the introspection changing the cache is logged, the lookup served from the cache is not
            assertThat(logCapture.messages(Level.TRACE)).containsExactly(
                    "Cached the introspection response for token [" + describe(jwt) + "] for PT30S " +
                            "(time to live PT5M, token expires at 2026-09-03T10:01:00Z, response expires at 2026-09-03T10:00:30Z).");
        }
    }

    @Test
    void getOrIntrospect_whenNeitherTokenNorResponseExpires_thenLogsTimeToLiveAsLifetime() {
        Jwt jwt = createJwt("token-1", null);

        try (LogCapture logCapture = LogCapture.of(CaffeineJeapTokenIntrospectionCache.class)) {
            cache.getOrIntrospect(keyOf(jwt), jwt, new CountingIntrospection());

            assertThat(logCapture.messages(Level.TRACE)).containsExactly(
                    "Cached the introspection response for token [" + describe(jwt) + "] for " + UNBOUNDED_LIFETIME + ".");
        }
    }

    @Test
    void introspect_whenTokenActive_thenLogsReplacementOfCachedResponse() {
        Jwt jwt = createJwt("token-1", null);
        CountingIntrospection introspection = new CountingIntrospection();
        cache.getOrIntrospect(keyOf(jwt), jwt, introspection);

        try (LogCapture logCapture = LogCapture.of(CaffeineJeapTokenIntrospectionCache.class)) {
            cache.introspect(keyOf(jwt), jwt, introspection);

            assertThat(logCapture.messages(Level.TRACE)).containsExactly(
                    "Replaced the cached introspection response for token [" + describe(jwt) + "] by the fresh response, cached for " + UNBOUNDED_LIFETIME + ".");
        }
    }

    @Test
    void introspect_whenNoCachedResponse_thenLogsCachingOfFreshResponse() {
        Jwt jwt = createJwt("token-1", null);

        try (LogCapture logCapture = LogCapture.of(CaffeineJeapTokenIntrospectionCache.class)) {
            cache.introspect(keyOf(jwt), jwt, new CountingIntrospection());

            assertThat(logCapture.messages(Level.TRACE)).containsExactly(
                    "Cached the introspection response for token [" + describe(jwt) + "] for " + UNBOUNDED_LIFETIME + ".");
        }
    }

    @Test
    void introspect_whenTokenInactive_thenLogsRemovalOfCachedResponse() {
        Jwt jwt = createJwt("token-1", null);
        JeapTokenIntrospectionCacheKey key = keyOf(jwt);
        cache.getOrIntrospect(key, jwt, new CountingIntrospection());
        Supplier<Map<String, Object>> inactiveIntrospection = () -> {
            throw new JeapIntrospectionInvalidTokenException();
        };

        try (LogCapture logCapture = LogCapture.of(CaffeineJeapTokenIntrospectionCache.class)) {
            assertThatThrownBy(() -> cache.introspect(key, jwt, inactiveIntrospection)).isInstanceOf(JeapIntrospectionInvalidTokenException.class);

            assertThat(logCapture.messages(Level.TRACE)).containsExactly(
                    "Removed the cached introspection response for token [" + describe(jwt) + "], as the fresh introspection reported the token inactive.");
        }
    }

    @Test
    void introspect_whenTokenInactiveAndNoCachedResponse_thenLogsNothing() {
        Jwt jwt = createJwt("token-1", null);
        JeapTokenIntrospectionCacheKey key = keyOf(jwt);
        Supplier<Map<String, Object>> inactiveIntrospection = () -> {
            throw new JeapIntrospectionInvalidTokenException();
        };

        try (LogCapture logCapture = LogCapture.of(CaffeineJeapTokenIntrospectionCache.class)) {
            assertThatThrownBy(() -> cache.introspect(key, jwt, inactiveIntrospection)).isInstanceOf(JeapIntrospectionInvalidTokenException.class);

            assertThat(logCapture.messages(Level.TRACE)).isEmpty();
        }
    }

    @Test
    void introspect_whenIntrospectionFails_thenLogsRetentionOfCachedResponse() {
        Jwt jwt = createJwt("token-1", null);
        JeapTokenIntrospectionCacheKey key = keyOf(jwt);
        cache.getOrIntrospect(key, jwt, new CountingIntrospection());
        Supplier<Map<String, Object>> failingIntrospection = () -> {
            throw new JeapIntrospectionException("Introspection endpoint not available");
        };

        try (LogCapture logCapture = LogCapture.of(CaffeineJeapTokenIntrospectionCache.class)) {
            assertThatThrownBy(() -> cache.introspect(key, jwt, failingIntrospection)).isInstanceOf(JeapIntrospectionException.class);

            assertThat(logCapture.messages(Level.TRACE)).containsExactly(
                    "Kept the cached introspection response for token [" + describe(jwt) + "], as the fresh introspection failed.");
        }
    }

    @Test
    void getOrIntrospect_whenMaximumSizeExceeded_thenLogsEvictions() {
        cache = createCache(2);
        CountingIntrospection introspection = new CountingIntrospection();

        try (LogCapture logCapture = LogCapture.of(CaffeineJeapTokenIntrospectionCache.class)) {
            for (int i = 0; i < 5; i++) {
                Jwt jwt = createJwt("token-" + i, null);
                cache.getOrIntrospect(keyOf(jwt), jwt, introspection);
            }
            assertThat(cache.estimatedSize()).isLessThanOrEqualTo(2);

            List<String> evictions = logCapture.messages(Level.TRACE).stream().filter(message -> message.startsWith("Evicted")).toList();
            assertThat(evictions).hasSizeGreaterThanOrEqualTo(3).allSatisfy(eviction -> assertThat(eviction)
                    .startsWith("Evicted the cached introspection response for token [issuer='" + ISSUER + "', jti='id-token-")
                    .endsWith("] from the cache to keep it within its maximum size of 2 entries."));
        }
    }

    @Test
    void getOrIntrospect_whenEntryExpired_thenLogsExpiry() {
        Jwt jwt = createJwt("token-1", null);
        cache.getOrIntrospect(keyOf(jwt), jwt, new CountingIntrospection());

        try (LogCapture logCapture = LogCapture.of(CaffeineJeapTokenIntrospectionCache.class)) {
            ticker.advance(TIME_TO_LIVE.plus(ONE_SECOND));
            assertThat(cache.estimatedSize()).isZero(); // the maintenance removes the expired entry

            assertThat(logCapture.messages(Level.TRACE)).containsExactly(
                    "The cached introspection response for token [" + describe(jwt) + "] has expired and has been removed from the cache.");
        }
    }

    private static boolean isBlocked(Thread thread) {
        Thread.State state = thread.getState();
        return state == Thread.State.BLOCKED || state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING;
    }

    private static void awaitRelease(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the latch.", e);
        }
    }

    private CaffeineJeapTokenIntrospectionCache createCache(long maximumSize) {
        return new CaffeineJeapTokenIntrospectionCache(
                new JeapTokenIntrospectionCacheConfiguration(ISSUER, maximumSize, TIME_TO_LIVE), clock, ticker);
    }

    private static Jwt createJwt(String tokenValue, Instant expiresAt) {
        Jwt.Builder builder = Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .issuer(ISSUER)
                .jti("id-" + tokenValue);
        if (expiresAt != null) {
            builder.expiresAt(expiresAt);
        }
        return builder.build();
    }

    private static JeapTokenIntrospectionCacheKey keyOf(Jwt jwt) {
        return JeapTokenIntrospectionCacheKey.of(jwt).orElseThrow();
    }

    /**
     * The token as described in the log messages: issuer, token id and a prefix of the token hash (the test tokens have no subject).
     */
    private static String describe(Jwt jwt) {
        return "issuer='" + ISSUER + "', jti='" + jwt.getId() + "', hash='" + keyOf(jwt).tokenHash().substring(0, 8) + "'";
    }

    private static class CountingIntrospection implements Supplier<Map<String, Object>> {

        private final AtomicInteger count = new AtomicInteger();
        private final Runnable beforeReturning;
        private final Map<String, Object> attributes;

        CountingIntrospection() {
            this(() -> {}, ATTRIBUTES);
        }

        CountingIntrospection(Runnable beforeReturning) {
            this(beforeReturning, ATTRIBUTES);
        }

        CountingIntrospection(Map<String, Object> attributes) {
            this(() -> {}, attributes);
        }

        private CountingIntrospection(Runnable beforeReturning, Map<String, Object> attributes) {
            this.beforeReturning = beforeReturning;
            this.attributes = attributes;
        }

        @Override
        public Map<String, Object> get() {
            count.incrementAndGet();
            beforeReturning.run();
            return attributes;
        }

        int count() {
            return count.get();
        }

    }

    private static class FakeTicker implements Ticker {

        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }

        /**
         * A clock advancing together with this ticker, starting at the given instant.
         */
        @SuppressWarnings("SameParameterValue")
        Clock clock(Instant start) {
            return new Clock() {
                @Override
                public ZoneId getZone() {
                    return ZoneOffset.UTC;
                }

                @Override
                public Clock withZone(ZoneId zone) {
                    return this;
                }

                @Override
                public Instant instant() {
                    return start.plusNanos(nanos.get());
                }
            };
        }

    }

}
