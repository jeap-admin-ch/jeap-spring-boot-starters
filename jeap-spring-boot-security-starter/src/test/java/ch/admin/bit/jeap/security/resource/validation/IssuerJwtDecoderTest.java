package ch.admin.bit.jeap.security.resource.validation;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IssuerJwtDecoderTest {

    private static final String ISSUER_A = "issue_a";
    private static final String ISSUER_B = "issue_b";

    private static final String TOKEN_ISSUER_A = createTokenIssuedByIssuer(ISSUER_A);
    private static final String TOKEN_ISSUER_B = createTokenIssuedByIssuer(ISSUER_B);

    private static final  Jwt JWT_ISSUER_A = Mockito.mock(Jwt.class);
    private static final Jwt JWT_ISSUER_B = Mockito.mock(Jwt.class);

    private static final JwtDecoder DECODER_ISSUER_A = Mockito.mock(JwtDecoder.class);
    private static final JwtDecoder DECODER_ISSUER_B = Mockito.mock(JwtDecoder.class);

    @BeforeEach
    void init() {
        Mockito.when(DECODER_ISSUER_A.decode(TOKEN_ISSUER_A)).thenReturn(JWT_ISSUER_A);
        Mockito.when(DECODER_ISSUER_B.decode(TOKEN_ISSUER_B)).thenReturn(JWT_ISSUER_B);
    }

    @Test
    void test_whenDecoderForIssuerRegistered_thenDecodeTokenSuccessfullyWithRegisteredDecoder() {
        final IssuerJwtDecoder decoder = IssuerJwtDecoder.builder().
                issuerDecoder(ISSUER_A, DECODER_ISSUER_A).
                build();

        final Jwt jwt = decoder.decode(TOKEN_ISSUER_A);

        assertThat(jwt).isEqualTo(JWT_ISSUER_A);
    }

    @Test
    void test_whenOnlyDecoderForIssuerARegistered_thenDecodeIssuerBTokenWithException() {
        final IssuerJwtDecoder decoder = IssuerJwtDecoder.builder().
                issuerDecoder(ISSUER_A, DECODER_ISSUER_A).
                build();

        assertThatThrownBy(() -> decoder.decode(TOKEN_ISSUER_B)).isInstanceOf(JeapTokenValidationException.class);
    }

    @Test
    void test_whenNoDecoderRegistered_thenException() {
        assertThatThrownBy(() -> IssuerJwtDecoder.builder().build()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void test_whenDecodersForIssuerAAndBRegistered_thenDecodeIssuerAAndBTokensWithCorrectDecoders() {
        final IssuerJwtDecoder decoder = IssuerJwtDecoder.builder().
                issuerDecoder(ISSUER_A, DECODER_ISSUER_A).
                issuerDecoder(ISSUER_B, DECODER_ISSUER_B).
                build();

        Jwt jwtIssuerA = decoder.decode(TOKEN_ISSUER_A);
        Jwt jwtIssuerB = decoder.decode(TOKEN_ISSUER_B);

        assertThat(jwtIssuerA).isEqualTo(JWT_ISSUER_A);
        assertThat(jwtIssuerB).isEqualTo(JWT_ISSUER_B);
    }

    @Test
    void test_whenTokenNotAJwt_thenThrowsBadJwtException() {
        final IssuerJwtDecoder decoder = IssuerJwtDecoder.builder().
                issuerDecoder(ISSUER_A, DECODER_ISSUER_A).
                build();

        assertThatThrownBy(() -> decoder.decode("not.a.jwt"))
                .isInstanceOf(BadJwtException.class);
    }

    private static String createTokenIssuedByIssuer(String issuer) {
        final JWTClaimsSet.Builder jwtClaimSetBuilder = new JWTClaimsSet.Builder();
        jwtClaimSetBuilder.issuer(issuer);
        PlainJWT jwt = new PlainJWT(jwtClaimSetBuilder.build());
        return jwt.serialize();
    }

}
