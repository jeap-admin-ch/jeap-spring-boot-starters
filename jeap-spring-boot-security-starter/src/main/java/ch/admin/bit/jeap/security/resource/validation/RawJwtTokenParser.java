package ch.admin.bit.jeap.security.resource.validation;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import java.text.ParseException;

class RawJwtTokenParser {

    static String extractIssuer(String token) throws ParseException {
        try {
            JWT jwt = JWTParser.parse(token);
            return jwt.getJWTClaimsSet().getIssuer();
        }
        catch (ParseException pex) {
            throw pex;
        }
        catch (Exception ex) {
            throw new IllegalArgumentException(String.format("Unable to extract issuer from token: %s", ex.getMessage()), ex);
        }
    }

}
