package ch.admin.bit.jeap.security.resource.validation;

import ch.admin.bit.jeap.security.resource.introspection.JeapIntrospectionException;
import ch.admin.bit.jeap.security.resource.introspection.JeapIntrospectionInvalidTokenException;
import ch.admin.bit.jeap.security.resource.introspection.JeapJwtIntrospection;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@RequiredArgsConstructor
class IntrospectingJwtDecoder implements JwtDecoder {

    private final JwtDecoder jwtDecoderDelegate;
    private final JeapJwtIntrospection jeapJwtIntrospection;

    @Override
    public Jwt decode(String token) {
        try {
            return jeapJwtIntrospection.introspectIfNeeded(jwtDecoderDelegate.decode(token));
        } catch (JeapIntrospectionInvalidTokenException inactiveException) {
            throw new JeapTokenValidationException("Token invalid according to its introspection.", inactiveException);
        } catch (JeapIntrospectionException introspectionException) {
            throw new JeapExternalTokenValidationException("Token introspection on auth server failed.", introspectionException);
        }
    }

}
