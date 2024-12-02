package ch.admin.bit.jeap.security.test.jws;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;

import lombok.RequiredArgsConstructor;

import java.time.ZonedDateTime;
import java.time.temporal.TemporalUnit;

/**
 * Provides a factory for JwsBuilders that prepopulates the created JwsBuilder instances with the
 * authorization server key from the test key provider.
 */
@RequiredArgsConstructor
public class JwsBuilderFactory {

    private final TestKeyProvider testKeyProvider;

    public JwsBuilder createBuilder(String jwtId, String issuer, ZonedDateTime expiry, ZonedDateTime notBefore, ZonedDateTime issuedAt, String subject, JeapAuthenticationContext context) {
        JwsBuilder jwsBuilder = JwsBuilder.create(jwtId, issuer, expiry, notBefore, issuedAt, subject, context);
        return addAuthServerKey(jwsBuilder);
    }

    public JwsBuilder createValidFromNowBuilder(String subject, JeapAuthenticationContext context, long validity, TemporalUnit temporalUnit) {
        JwsBuilder jwsBuilder = JwsBuilder.createValidFromNow(subject, context, validity, temporalUnit);
        return addAuthServerKey(jwsBuilder);
    }

    public JwsBuilder createValidForFixedLongPeriodBuilder(String subject, JeapAuthenticationContext context) {
        JwsBuilder jwsBuilder = JwsBuilder.createValidForFixedLongPeriod(subject, context);
        return addAuthServerKey(jwsBuilder);
    }

    private JwsBuilder addAuthServerKey(JwsBuilder jwsBuilder) {
        return jwsBuilder.withRsaKey(testKeyProvider.getAuthServerKey());
    }

}
