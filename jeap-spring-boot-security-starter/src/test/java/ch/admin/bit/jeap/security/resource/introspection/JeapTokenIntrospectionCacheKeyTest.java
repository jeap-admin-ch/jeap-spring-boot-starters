package ch.admin.bit.jeap.security.resource.introspection;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "SameParameterValue"})
class JeapTokenIntrospectionCacheKeyTest {

    private static final String ISSUER = "https://keycloak/auth/realm";
    private static final String TOKEN_ID = "0d2d5e7c-8e5a-4d63-9f1a-0b6f2b7c9e11";
    private static final String TOKEN_VALUE = "header.payload.signature";

    @Test
    void of_whenIssuerAndTokenIdPresent_thenKeyIdentifiesTokenWithoutRetainingItsValue() throws NoSuchAlgorithmException {
        Optional<JeapTokenIntrospectionCacheKey> key = JeapTokenIntrospectionCacheKey.of(createJwt(TOKEN_VALUE, ISSUER, TOKEN_ID));

        assertThat(key).isPresent();
        assertThat(key.get().issuer()).isEqualTo(ISSUER);
        assertThat(key.get().tokenId()).isEqualTo(TOKEN_ID);
        assertThat(key.get().tokenHash()).isEqualTo(sha256Base64Url(TOKEN_VALUE));
        assertThat(key.get().toString()).doesNotContain(TOKEN_VALUE);
    }

    @Test
    void of_whenSameToken_thenEqualKeys() {
        Optional<JeapTokenIntrospectionCacheKey> key = JeapTokenIntrospectionCacheKey.of(createJwt(TOKEN_VALUE, ISSUER, TOKEN_ID));
        Optional<JeapTokenIntrospectionCacheKey> sameKey = JeapTokenIntrospectionCacheKey.of(createJwt(TOKEN_VALUE, ISSUER, TOKEN_ID));

        assertThat(key).isEqualTo(sameKey);
        assertThat(key.get()).hasSameHashCodeAs(sameKey.get());
    }

    @Test
    void of_whenDifferentTokenValueWithSameTokenId_thenDifferentKeys() {
        Optional<JeapTokenIntrospectionCacheKey> key = JeapTokenIntrospectionCacheKey.of(createJwt(TOKEN_VALUE, ISSUER, TOKEN_ID));
        Optional<JeapTokenIntrospectionCacheKey> otherKey = JeapTokenIntrospectionCacheKey.of(createJwt(TOKEN_VALUE + "x", ISSUER, TOKEN_ID));

        assertThat(key).isNotEqualTo(otherKey);
    }

    @Test
    void of_whenDifferentTokenIdWithSameTokenValue_thenDifferentKeys() {
        Optional<JeapTokenIntrospectionCacheKey> key = JeapTokenIntrospectionCacheKey.of(createJwt(TOKEN_VALUE, ISSUER, TOKEN_ID));
        Optional<JeapTokenIntrospectionCacheKey> otherKey = JeapTokenIntrospectionCacheKey.of(createJwt(TOKEN_VALUE, ISSUER, "other-id"));

        assertThat(key).isNotEqualTo(otherKey);
    }

    @Test
    void of_whenTokenIdMissing_thenEmpty() {
        Jwt jwt = Jwt.withTokenValue(TOKEN_VALUE).header("alg", "none").issuer(ISSUER).build();

        assertThat(JeapTokenIntrospectionCacheKey.of(jwt)).isEmpty();
    }

    @Test
    void of_whenTokenIdBlank_thenEmpty() {
        assertThat(JeapTokenIntrospectionCacheKey.of(createJwt(TOKEN_VALUE, ISSUER, " "))).isEmpty();
    }

    @Test
    void of_whenIssuerMissing_thenEmpty() {
        Jwt jwt = Jwt.withTokenValue(TOKEN_VALUE).header("alg", "none").jti(TOKEN_ID).build();

        assertThat(JeapTokenIntrospectionCacheKey.of(jwt)).isEmpty();
    }

    private static Jwt createJwt(String tokenValue, String issuer, String tokenId) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .issuer(issuer)
                .jti(tokenId)
                .build();
    }

    private static String sha256Base64Url(String value) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

}
