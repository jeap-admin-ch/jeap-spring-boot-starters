package ch.admin.bit.jeap.security.resource.introspection;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.Ticker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Token introspection cache local to the instance, based on Caffeine. Entries expire after the configured time to live,
 * or earlier if the token they belong to or the introspection response itself expires earlier, and the cache is
 * bounded by the configured maximum size. Cached responses are stored as unmodifiable deep copies. All introspections
 * of a token are executed one after the other under the token's cache lock, so concurrent lookups of the same token
 * are coalesced into a single introspection request and the most recent introspection result always wins.
 * <p>
 * Every change of the cache content is logged on TRACE. Note that Caffeine removes expired entries lazily during its
 * maintenance, so the expiry of an entry may be logged after a lookup that already replaced it.
 */
@Slf4j
class CaffeineJeapTokenIntrospectionCache implements JeapTokenIntrospectionCache {

    private final Cache<JeapTokenIntrospectionCacheKey, CachedIntrospection> cache;
    private final Duration timeToLive;
    private final Clock clock;

    CaffeineJeapTokenIntrospectionCache(JeapTokenIntrospectionCacheConfiguration config) {
        this(config, Clock.systemUTC(), Ticker.systemTicker());
    }

    // The clock determines the remaining lifetime of a token when its introspection response gets cached, the ticker
    // drives the expiry of the cache entries. Both can be replaced for testing purposes.
    CaffeineJeapTokenIntrospectionCache(JeapTokenIntrospectionCacheConfiguration config, Clock clock, Ticker ticker) {
        this.timeToLive = config.timeToLive();
        this.clock = clock;
        this.cache = Caffeine.newBuilder()
                .maximumSize(config.maximumSize())
                .expireAfter(new TokenBoundExpiry())
                .evictionListener(new EvictionLogger(config.maximumSize()))
                .ticker(ticker)
                .build();
    }

    @Override
    public Map<String, Object> getOrIntrospect(JeapTokenIntrospectionCacheKey key, Jwt jwt, Supplier<Map<String, Object>> introspection) {
        // Caffeine computes the value at most once per key under the key's lock, coalescing concurrent lookups of the
        // same token into one introspection. An exception thrown by the introspection is propagated and no entry is
        // recorded.
        CachedIntrospection cachedIntrospection = cache.get(key, k -> introspectAndCache(jwt, k, introspection, null));
        return cachedIntrospection.attributes();
    }

    @Override
    public Map<String, Object> introspect(JeapTokenIntrospectionCacheKey key, Jwt jwt, Supplier<Map<String, Object>> introspection) {
        // The refresh runs under the key's lock as well, i.e. the introspections of a token are executed one after the
        // other and the result of an older introspection can never overwrite the result of a newer one.
        Refresh refresh = new Refresh(TokenDescription.of(jwt, key),
                existing -> introspectAndCache(jwt, key, introspection, existing));
        CachedIntrospection cachedIntrospection = cache.asMap().compute(key, refresh);
        if (refresh.foundTokenInactive()) {
            throw refresh.inactiveToken();
        }
        return Objects.requireNonNull(cachedIntrospection).attributes();
    }

    /**
     * Introspects the token with the given introspection and prepares the response for caching. The existing cached
     * response of the token, null if there is none, only serves to describe the change of the cache in the log.
     */
    private CachedIntrospection introspectAndCache(Jwt jwt, JeapTokenIntrospectionCacheKey key,
                                                   Supplier<Map<String, Object>> introspection, CachedIntrospection existing) {
        Map<String, Object> attributes = unmodifiableDeepCopy(introspection.get());
        EntryLifetime lifetime = entryLifetime(jwt, responseExpiry(attributes));
        if (existing != null) {
            log.trace("Replaced the cached introspection response for token [{}] by the fresh response, cached for {}.",
                    TokenDescription.of(jwt, key), lifetime);
        } else {
            log.trace("Cached the introspection response for token [{}] for {}.", TokenDescription.of(jwt, key), lifetime);
        }
        return new CachedIntrospection(attributes, lifetime.duration());
    }

    /**
     * The number of entries currently in the cache after pending maintenance work has been performed.
     */
    long estimatedSize() {
        cache.cleanUp();
        return cache.estimatedSize();
    }

    /**
     * The lifetime of a cache entry is the configured time to live, bounded by the expiry of the token and by the
     * expiry of the introspection response (RFC 7662 requires not to rely on a cached response past its 'exp').
     */
    private EntryLifetime entryLifetime(Jwt jwt, Instant responseExpiry) {
        Duration lifetime = boundByExpiry(timeToLive, jwt.getExpiresAt());
        lifetime = boundByExpiry(lifetime, responseExpiry);
        return new EntryLifetime(lifetime, timeToLive, jwt.getExpiresAt(), responseExpiry);
    }

    private Duration boundByExpiry(Duration lifetime, Instant expiry) {
        if (expiry == null) {
            return lifetime;
        }
        Duration remainingLifetime = Duration.between(clock.instant(), expiry);
        return remainingLifetime.compareTo(lifetime) < 0 ? remainingLifetime : lifetime;
    }

    /**
     * The expiry of an introspection response as declared by its 'exp' attribute, provided as an Instant by Spring's
     * token introspector or as epoch seconds by other introspectors. Returns null if the response does not declare
     * an expiry.
     */
    private static Instant responseExpiry(Map<String, Object> attributes) {
        Object expiry = attributes.get(OAuth2TokenIntrospectionClaimNames.EXP);
        if (expiry instanceof Instant instant) {
            return instant;
        }
        if (expiry instanceof Number epochSeconds) {
            return Instant.ofEpochSecond(epochSeconds.longValue());
        }
        return null;
    }

    /**
     * A cached response is shared by all requests presenting the same token. It is therefore stored as a deep copy
     * that cannot be modified, nested maps and collections included, so that a request cannot alter the response
     * served to other requests.
     */
    private static Map<String, Object> unmodifiableDeepCopy(Map<String, Object> attributes) {
        Map<String, Object> copy = new LinkedHashMap<>();
        attributes.forEach((name, value) -> copy.put(name, unmodifiableDeepCopyOfValue(value)));
        return Collections.unmodifiableMap(copy);
    }

    private static Object unmodifiableDeepCopyOfValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            map.forEach((key, element) -> copy.put(key, unmodifiableDeepCopyOfValue(element)));
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof Set<?> set) {
            Set<Object> copy = new LinkedHashSet<>();
            set.forEach(element -> copy.add(unmodifiableDeepCopyOfValue(element)));
            return Collections.unmodifiableSet(copy);
        }
        if (value instanceof Collection<?> collection) {
            List<Object> copy = new ArrayList<>();
            collection.forEach(element -> copy.add(unmodifiableDeepCopyOfValue(element)));
            return Collections.unmodifiableList(copy);
        }
        return value;
    }

    private record CachedIntrospection(Map<String, Object> attributes, Duration lifetime) {}

    /**
     * The lifetime of a cache entry together with the bounds it has been derived from, described for the log.
     */
    private record EntryLifetime(Duration duration, Duration timeToLive, Instant tokenExpiry, Instant responseExpiry) {

        @Override
        public String toString() {
            return "%s (time to live %s, %s, %s)".formatted(duration, timeToLive,
                    describeExpiry("token", tokenExpiry), describeExpiry("response", responseExpiry));
        }

        private static String describeExpiry(String subject, Instant expiry) {
            return expiry != null ? subject + " expires at " + expiry : subject + " declares no expiry";
        }

    }

    /**
     * Refreshes the cached response of a token, as a remapping function of the cache running the given introspection
     * (which receives the existing cached response, null if there is none, and returns the fresh one to cache):
     * returning a response replaces the cached response, returning null removes it, and throwing leaves it untouched.
     * An inactive token removes the cached response and must still be reported to the caller. As the function cannot
     * both remove the response and throw, it remembers the inactive token for the caller to check after the refresh.
     */
    private static final class Refresh implements BiFunction<JeapTokenIntrospectionCacheKey, CachedIntrospection, CachedIntrospection> {

        private final TokenDescription token;
        private final UnaryOperator<CachedIntrospection> introspection;
        private JeapIntrospectionInvalidTokenException inactiveToken;

        private Refresh(TokenDescription token, UnaryOperator<CachedIntrospection> introspection) {
            this.token = token;
            this.introspection = introspection;
        }

        @Override
        public CachedIntrospection apply(JeapTokenIntrospectionCacheKey ignoredKey, CachedIntrospection existing) {
            // A refresh replaces the cached response regardless of the existing one: the fresh result is the most
            // recent information by construction. After a failure the existing response is kept by throwing, not by
            // returning it, as returning it would count as an update and restart the lifetime of the entry.
            try {
                return introspection.apply(existing);
            } catch (JeapIntrospectionInvalidTokenException e) {
                inactiveToken = e;
                logRemoval(existing);
                return null;
            } catch (RuntimeException e) {
                logRetention(existing);
                throw e;
            }
        }

        private void logRemoval(CachedIntrospection existing) {
            if (existing != null) {
                log.trace("Removed the cached introspection response for token [{}], as the fresh introspection reported the token inactive.", token);
            }
        }

        private void logRetention(CachedIntrospection existing) {
            if (existing != null) {
                log.trace("Kept the cached introspection response for token [{}], as the fresh introspection failed.", token);
            }
        }

        boolean foundTokenInactive() {
            return inactiveToken != null;
        }

        JeapIntrospectionInvalidTokenException inactiveToken() {
            return inactiveToken;
        }

    }

    private static class TokenBoundExpiry implements Expiry<JeapTokenIntrospectionCacheKey, CachedIntrospection> {

        @Override
        public long expireAfterCreate(JeapTokenIntrospectionCacheKey key, CachedIntrospection value, long currentTime) {
            return toNanos(value.lifetime());
        }

        @Override
        public long expireAfterUpdate(JeapTokenIntrospectionCacheKey key, CachedIntrospection value, long currentTime, long currentDuration) {
            return toNanos(value.lifetime());
        }

        @Override
        public long expireAfterRead(JeapTokenIntrospectionCacheKey key, CachedIntrospection value, long currentTime, long currentDuration) {
            return currentDuration;
        }

        private static long toNanos(Duration lifetime) {
            return lifetime.isNegative() ? 0L : lifetime.toNanos();
        }

    }

    /**
     * Logs the evictions of the cache. Caffeine invokes the listener as part of the eviction, i.e. synchronously and
     * only for entries evicted by the cache itself (expired or removed to keep the maximum size), not for entries
     * replaced or removed by a refresh.
     */
    private record EvictionLogger(long maximumSize) implements RemovalListener<JeapTokenIntrospectionCacheKey, CachedIntrospection> {

        @Override
        public void onRemoval(JeapTokenIntrospectionCacheKey key, CachedIntrospection value, RemovalCause cause) {
            switch (cause) {
                case SIZE -> log.trace("Evicted the cached introspection response for token [{}] from the cache to keep it within its maximum size of {} entries.",
                        TokenDescription.of(key), maximumSize);
                case EXPIRED -> log.trace("The cached introspection response for token [{}] has expired and has been removed from the cache.",
                        TokenDescription.of(key));
                default -> log.trace("Removed the cached introspection response for token [{}] from the cache ({}).",
                        TokenDescription.of(key), cause);
            }
        }

    }

}
