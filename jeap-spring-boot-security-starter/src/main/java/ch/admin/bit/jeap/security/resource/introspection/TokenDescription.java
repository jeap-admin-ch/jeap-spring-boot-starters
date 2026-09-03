package ch.admin.bit.jeap.security.resource.introspection;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.StringJoiner;

/**
 * Describes a token in the log messages of the token introspection without revealing the token value: by its issuer,
 * its subject, its token id (jti claim) and a prefix of the SHA-256 hash of the token value as used in the cache key.
 * The token id and the hash prefix identify the cache entry of the token, the subject relates the message to the
 * other log messages of the token introspection. Parts that are unknown or not available are left out. The
 * description is formatted only when a log message is actually written, so it can be passed to the logger as a
 * message argument at negligible cost even if the level of the logger is not enabled.
 *
 * @param issuer    The issuer of the token, {@code null} if unknown
 * @param subject   The subject of the token, {@code null} if unknown
 * @param tokenId   The token id (jti claim) of the token, {@code null} if the token has none
 * @param tokenHash The SHA-256 hash of the token value as used in the cache key, {@code null} if not available
 */
record TokenDescription(String issuer, String subject, String tokenId, String tokenHash) {

    private static final int HASH_PREFIX_LENGTH = 8;

    /**
     * Describe a token that is not cached.
     */
    static TokenDescription of(Jwt jwt) {
        String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
        return new TokenDescription(issuer, jwt.getSubject(), jwt.getId(), null);
    }

    /**
     * Describe a token that is cached.
     */
    static TokenDescription of(Jwt jwt, JeapTokenIntrospectionCacheKey key) {
        return new TokenDescription(key.issuer(), jwt.getSubject(), key.tokenId(), key.tokenHash());
    }

    /**
     * Describe a token that was cached but itself is no longer at hand.
     */
    static TokenDescription of(JeapTokenIntrospectionCacheKey key) {
        return new TokenDescription(key.issuer(), null, key.tokenId(), key.tokenHash());
    }

    @Override
    public String toString() {
        StringJoiner description = new StringJoiner(", ");
        if (issuer != null) {
            description.add("issuer='" + issuer + "'");
        }
        if (subject != null) {
            description.add("subject='" + subject + "'");
        }
        if (tokenId != null) {
            description.add("jti='" + tokenId + "'");
        }
        if (tokenHash != null) {
            description.add("hash='" + tokenHash.substring(0, Math.min(HASH_PREFIX_LENGTH, tokenHash.length())) + "'");
        }
        return description.toString();
    }

}
