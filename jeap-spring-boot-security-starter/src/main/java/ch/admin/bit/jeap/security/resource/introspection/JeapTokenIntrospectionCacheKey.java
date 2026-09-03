package ch.admin.bit.jeap.security.resource.introspection;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

/**
 * Key identifying a cached token introspection response. The key combines the issuer, the token id (jti claim)
 * assigned by the authorization server and a SHA-256 hash of the complete token value. The token id ensures that a
 * hash collision between tokens with different ids cannot lead to a mix-up of their responses, while the hash binds
 * the entry to the exact token value presented. The token value itself is not part of the key, so no bearer tokens
 * are retained by a cache using this key. Tokens without an issuer or a token id are deliberately not cached, as this
 * additional safeguard is not available for them (see {@link #of(Jwt)}). Note that the JWT profile for OAuth 2.0
 * access tokens (RFC 9068) makes the jti claim mandatory.
 *
 * @param issuer    The issuer of the token
 * @param tokenId   The id of the token (jti claim)
 * @param tokenHash The SHA-256 hash of the token value (Base64 URL encoded)
 */
public record JeapTokenIntrospectionCacheKey(String issuer, String tokenId, String tokenHash) {

    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Create the cache key for the given token.
     *
     * @param jwt The token
     * @return The cache key, or empty if the token is not cacheable because it lacks an issuer or a token id.
     */
    public static Optional<JeapTokenIntrospectionCacheKey> of(Jwt jwt) {
        if (jwt.getIssuer() == null || !StringUtils.hasText(jwt.getId()) || !StringUtils.hasText(jwt.getTokenValue())) {
            return Optional.empty();
        }
        return Optional.of(new JeapTokenIntrospectionCacheKey(jwt.getIssuer().toString(), jwt.getId(), hash(jwt.getTokenValue())));
    }

    private static String hash(String tokenValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(tokenValue.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hash algorithm %s is not available.".formatted(HASH_ALGORITHM), e);
        }
    }

}
