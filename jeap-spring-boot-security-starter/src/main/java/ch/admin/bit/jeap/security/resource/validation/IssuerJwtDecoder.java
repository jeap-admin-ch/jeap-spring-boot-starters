package ch.admin.bit.jeap.security.resource.validation;

import lombok.Builder;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.Map;

/**
 * This class implements a JwtDecoder that delegates the decoding to one of the configured JwtDecoder instances
 * depending on the issuer of the raw JWT token to decode.
 */
@Builder
@Slf4j
class IssuerJwtDecoder implements JwtDecoder {

    @Singular("issuerDecoder")
    private final Map<String, JwtDecoder> issuerDecoderMap;

    private IssuerJwtDecoder(Map<String, JwtDecoder> issuerDecoderMap) {
        if (issuerDecoderMap.isEmpty()) {
            throw new IllegalStateException("At least one decoder must be configured.");
        }
        this.issuerDecoderMap = issuerDecoderMap;
    }

    @Override
    public Jwt decode(final String token) {
        final String issuer = RawJwtTokenParser.extractIssuer(token);
        final JwtDecoder decoder = issuerDecoderMap.get(issuer);
        if (decoder != null) {
            log.debug("Decoding a token from issuer '{}'.", issuer);
            Jwt jwt = decoder.decode(token);
            log.debug("Decoded token from issuer '{}' for subject '{}'.", jwt.getIssuer(), jwt.getSubject());
            return jwt;
        } else {
            throw new JeapTokenValidationException("Unsupported issuer '" + issuer + "'. There is no JwtDecoder configured for it.");
        }
    }

}
