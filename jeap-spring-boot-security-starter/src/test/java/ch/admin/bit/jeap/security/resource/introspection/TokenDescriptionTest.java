package ch.admin.bit.jeap.security.resource.introspection;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class TokenDescriptionTest {

    private static final String ISSUER = "https://keycloak/auth/realm";
    private static final String SUBJECT = "1234567890";
    private static final String TOKEN_ID = "token-id";
    private static final String TOKEN_VALUE = "token-value";

    @Test
    void of_whenJwt_thenIssuerSubjectAndTokenIdWithoutHash() {
        Jwt jwt = createJwt().subject(SUBJECT).jti(TOKEN_ID).build();

        assertThat(TokenDescription.of(jwt)).hasToString("issuer='" + ISSUER + "', subject='" + SUBJECT + "', jti='" + TOKEN_ID + "'");
    }

    @Test
    void of_whenJwtAndCacheKey_thenIssuerSubjectTokenIdAndHashPrefix() {
        Jwt jwt = createJwt().subject(SUBJECT).jti(TOKEN_ID).build();
        JeapTokenIntrospectionCacheKey key = JeapTokenIntrospectionCacheKey.of(jwt).orElseThrow();

        String description = TokenDescription.of(jwt, key).toString();

        assertThat(description).isEqualTo("issuer='" + ISSUER + "', subject='" + SUBJECT + "', jti='" + TOKEN_ID + "', hash='" + key.tokenHash().substring(0, 8) + "'");
        assertThat(description).doesNotContain(key.tokenHash()).doesNotContain(TOKEN_VALUE);
    }

    @Test
    void of_whenCacheKeyOnly_thenIssuerTokenIdAndHashPrefixWithoutSubject() {
        Jwt jwt = createJwt().subject(SUBJECT).jti(TOKEN_ID).build();
        JeapTokenIntrospectionCacheKey key = JeapTokenIntrospectionCacheKey.of(jwt).orElseThrow();

        assertThat(TokenDescription.of(key)).hasToString("issuer='" + ISSUER + "', jti='" + TOKEN_ID + "', hash='" + key.tokenHash().substring(0, 8) + "'");
    }

    @Test
    void of_whenJwtWithoutSubjectAndTokenId_thenIssuerOnly() {
        Jwt jwt = createJwt().build();

        assertThat(TokenDescription.of(jwt)).hasToString("issuer='" + ISSUER + "'");
    }

    private static Jwt.Builder createJwt() {
        return Jwt.withTokenValue(TOKEN_VALUE)
                .header("alg", "none")
                .issuer(ISSUER);
    }

}
