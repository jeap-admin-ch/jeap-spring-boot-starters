package ch.admin.bit.jeap.security.resource.validation;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReactiveIssuerJwtDecoderTest {

    private static final String ISSUER_A = "issue_a";
    private static final String ISSUER_B = "issue_b";

    private static final String TOKEN_ISSUER_A = createTokenIssuedByIssuer(ISSUER_A);
    private static final String TOKEN_ISSUER_B = createTokenIssuedByIssuer(ISSUER_B);

    private static final  Jwt JWT_ISSUER_A = Mockito.mock(Jwt.class);
    private static final Jwt JWT_ISSUER_B = Mockito.mock(Jwt.class);

    private static final ReactiveJwtDecoder DECODER_ISSUER_A = Mockito.mock(ReactiveJwtDecoder.class);
    private static final ReactiveJwtDecoder DECODER_ISSUER_B = Mockito.mock(ReactiveJwtDecoder.class);

    @BeforeEach
    void init() {
        Mockito.when(DECODER_ISSUER_A.decode(TOKEN_ISSUER_A)).thenReturn(Mono.just(JWT_ISSUER_A));
        Mockito.when(DECODER_ISSUER_B.decode(TOKEN_ISSUER_B)).thenReturn(Mono.just(JWT_ISSUER_B));
    }

    @Test
    void test_whenDecoderForIssuerRegistered_thenDecodeTokenSuccessfullyWithRegisteredDecoder() {
        final ReactiveIssuerJwtDecoder decoder = ReactiveIssuerJwtDecoder.builder().
                issuerDecoder(ISSUER_A, DECODER_ISSUER_A).
                build();

        final Jwt jwt = decoder.decode(TOKEN_ISSUER_A).block();

        assertThat(jwt).isEqualTo(JWT_ISSUER_A);
    }

    @Test
    void test_whenOnlyDecoderForIssuerARegistered_thenDecodeIssuerBTokenWithException() {
        final ReactiveIssuerJwtDecoder decoder = ReactiveIssuerJwtDecoder.builder().
                issuerDecoder(ISSUER_A, DECODER_ISSUER_A).
                build();

        assertThatThrownBy(() -> decoder.decode(TOKEN_ISSUER_B).block()).isInstanceOf(JeapTokenValidationException.class);
    }

    @Test
    void test_whenNoDecoderRegistered_theException() {
        assertThatThrownBy(() -> ReactiveIssuerJwtDecoder.builder().build()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void test_whenDecodersForIssuerAAndBRegistered_thenDecodeIssuerAAndBTokensWithCorrectDecoders() {
        final ReactiveIssuerJwtDecoder decoder = ReactiveIssuerJwtDecoder.builder().
                issuerDecoder(ISSUER_A, DECODER_ISSUER_A).
                issuerDecoder(ISSUER_B, DECODER_ISSUER_B).
                build();

        Jwt jwtIssuerA = decoder.decode(TOKEN_ISSUER_A).block();
        Jwt jwtIssuerB = decoder.decode(TOKEN_ISSUER_B).block();

        assertThat(jwtIssuerA).isEqualTo(JWT_ISSUER_A);
        assertThat(jwtIssuerB).isEqualTo(JWT_ISSUER_B);
    }

    private static String createTokenIssuedByIssuer(String issuer) {
        final JWTClaimsSet.Builder jwtClaimSetBuilder = new JWTClaimsSet.Builder();
        jwtClaimSetBuilder.issuer(issuer);
        PlainJWT jwt = new PlainJWT(jwtClaimSetBuilder.build());
        return jwt.serialize();
    }

}
