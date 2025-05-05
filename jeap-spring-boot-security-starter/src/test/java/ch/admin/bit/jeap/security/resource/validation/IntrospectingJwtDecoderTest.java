package ch.admin.bit.jeap.security.resource.validation;

import ch.admin.bit.jeap.security.resource.introspection.JeapIntrospectionException;
import ch.admin.bit.jeap.security.resource.introspection.JeapIntrospectionInvalidTokenException;
import ch.admin.bit.jeap.security.resource.introspection.JeapJwtIntrospection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IntrospectingJwtDecoderTest {

    private static final String TOKEN = "test-token";
    private static final Jwt ORIGINAL_JWT = createJwt();
    private static final Jwt INTROSPECTED_JWT = createIntrospectedJwt();

    private JwtDecoder jwtDecoderDelegate;
    private JeapJwtIntrospection jeapJwtIntrospection;
    private IntrospectingJwtDecoder introspectingJwtDecoder;

    @BeforeEach
    void setUp() {
        jwtDecoderDelegate = Mockito.mock(JwtDecoder.class);
        jeapJwtIntrospection = Mockito.mock(JeapJwtIntrospection.class);
        introspectingJwtDecoder = new IntrospectingJwtDecoder(jwtDecoderDelegate, jeapJwtIntrospection);
    }

    @Test
    void decode_shouldDelegateToJwtDecoderAndIntrospect() {
        when(jwtDecoderDelegate.decode(TOKEN)).thenReturn(ORIGINAL_JWT);
        when(jeapJwtIntrospection.introspectIfNeeded(ORIGINAL_JWT)).thenReturn(INTROSPECTED_JWT);

        Jwt result = introspectingJwtDecoder.decode(TOKEN);

        assertThat(result).isEqualTo(INTROSPECTED_JWT);
        verify(jwtDecoderDelegate).decode(TOKEN);
        verify(jeapJwtIntrospection).introspectIfNeeded(ORIGINAL_JWT);
    }

    @Test
    void decode_whenIntrospectionNotNeeded_shouldReturnOriginalJwt() {
        when(jwtDecoderDelegate.decode(TOKEN)).thenReturn(ORIGINAL_JWT);
        when(jeapJwtIntrospection.introspectIfNeeded(ORIGINAL_JWT)).thenReturn(ORIGINAL_JWT);

        Jwt result = introspectingJwtDecoder.decode(TOKEN);

        assertThat(result).isEqualTo(ORIGINAL_JWT);
        verify(jwtDecoderDelegate).decode(TOKEN);
        verify(jeapJwtIntrospection).introspectIfNeeded(ORIGINAL_JWT);
    }

    @Test
    void decode_whenIntrospectionThrowsInvalidTokenException_shouldThrowJeapTokenValidationExceptionInvalid() {
        when(jwtDecoderDelegate.decode(TOKEN)).thenReturn(ORIGINAL_JWT);
        final JeapIntrospectionInvalidTokenException exception = new JeapIntrospectionInvalidTokenException();
        when(jeapJwtIntrospection.introspectIfNeeded(ORIGINAL_JWT)).thenThrow(exception);

        assertThatThrownBy(() -> introspectingJwtDecoder.decode(TOKEN))
                .isInstanceOf(JeapTokenValidationException.class)
                .hasMessageContainingAll("Token invalid according to its introspection.");
    }

    @Test
    void decode_whenIntrospectionThrowsException_shouldThrowJeapExternalTokenValidationException() {
        when(jwtDecoderDelegate.decode(TOKEN)).thenReturn(ORIGINAL_JWT);
        JeapIntrospectionException exception = new JeapIntrospectionException("Introspection failed");
        when(jeapJwtIntrospection.introspectIfNeeded(ORIGINAL_JWT)).thenThrow(exception);

        assertThatThrownBy(() -> introspectingJwtDecoder.decode(TOKEN))
                .isInstanceOf(JeapExternalTokenValidationException.class)
                .hasMessageContainingAll("Token introspection on auth server failed.");
    }

    @Test
    void decode_whenDelegateThrowsException_shouldPropagateException() {
        RuntimeException exception = new RuntimeException("Decoding failed");
        when(jwtDecoderDelegate.decode(TOKEN)).thenThrow(exception);

        assertThatThrownBy(() -> introspectingJwtDecoder.decode(TOKEN))
                .isEqualTo(exception);
        verify(jeapJwtIntrospection, never()).introspectIfNeeded(any());
    }

    private static Jwt createJwt() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "none");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "test-subject");
        claims.put("iss", "test-issuer");

        return new Jwt(TOKEN, Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
    }

    private static Jwt createIntrospectedJwt() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "none");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "test-subject");
        claims.put("iss", "test-issuer");
        claims.put("active", true);
        claims.put("introspected", true);

        return new Jwt(TOKEN, Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
    }
}
