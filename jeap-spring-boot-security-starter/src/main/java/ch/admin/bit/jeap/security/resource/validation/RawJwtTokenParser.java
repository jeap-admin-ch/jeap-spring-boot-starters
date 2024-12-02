package ch.admin.bit.jeap.security.resource.validation;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

class RawJwtTokenParser {

    static String extractIssuer(String token) {
        try {
            JWT jwt = JWTParser.parse(token);
            return jwt.getJWTClaimsSet().getIssuer();
        } catch (Exception ex) {
            throw new IllegalArgumentException(String.format("Unable to extract issuer from token: %s", ex.getMessage()), ex);
        }
    }
}
