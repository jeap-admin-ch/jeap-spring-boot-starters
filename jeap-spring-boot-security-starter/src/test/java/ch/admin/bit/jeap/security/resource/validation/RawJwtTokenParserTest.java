package ch.admin.bit.jeap.security.resource.validation;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RawJwtTokenParserTest {

    @Test
    void test_WhenNotAJwtToken_ThenThrowsParseException() {
        final String token = "not.a.jwt";
        assertThatThrownBy(() -> RawJwtTokenParser.extractIssuer(token)).
                isInstanceOf(ParseException.class);
    }

    @Test
    @SneakyThrows
    void test_WhenJwtTokenWithIssuer_ThenReturnsIssuer() {
        final String issClaim = "test-issuer";
        final JWTClaimsSet claims = new JWTClaimsSet.Builder().
                issuer(issClaim).
                subject("test-subject").
                expirationTime(Date.from(Instant.now().plusSeconds(60))).
                build();
        final JWT jwt = new PlainJWT(claims);
        final String token = jwt.serialize();

        String issuer = RawJwtTokenParser.extractIssuer(token);

        assertThat(issuer).isEqualTo(issClaim);
    }

    @Test
    @SneakyThrows
    void test_WhenJwtTokenWithoutIssuer_ThenReturnsNull() {
        final JWTClaimsSet claims = new JWTClaimsSet.Builder().
                subject("test-subject").
                expirationTime(Date.from(Instant.now().plusSeconds(60))).
                build();
        final JWT jwt = new PlainJWT(claims);
        final String token = jwt.serialize();

        String issuer = RawJwtTokenParser.extractIssuer(token);

        assertThat(issuer).isNull();
    }

}
