package ch.admin.bit.jeap.security.resource.validation;

import lombok.Builder;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 *  This class implements a JwtDecoder that delegates the decoding to one of the configured JwtDecoder instances
 *  depending on the issuer of the raw JWT token to decode.
 */
@Builder
@Slf4j
class ReactiveIssuerJwtDecoder implements ReactiveJwtDecoder {

    @Singular("issuerDecoder")
    private final Map<String, ReactiveJwtDecoder> issuerDecoderMap;

    private ReactiveIssuerJwtDecoder(Map<String, ReactiveJwtDecoder> issuerDecoderMap) {
        if (issuerDecoderMap.isEmpty()) {
            throw new IllegalStateException("At least one decoder must be configured.");
        }
        this.issuerDecoderMap = issuerDecoderMap;
    }

    @Override
    public Mono<Jwt> decode(final String token) throws JwtException {
        final String issuer = RawJwtTokenParser.extractIssuer(token);
        final ReactiveJwtDecoder decoder = issuerDecoderMap.get(issuer);
        if (decoder != null) {
            log.debug("Decoding a token from issuer '{}' ", issuer);
            return decoder.decode(token).
                    doOnNext(jwt -> log.debug("Decoded token from issuer '{}' for subject '{}'.", jwt.getIssuer(), jwt.getSubject()));
        } else {
            throw new JeapTokenValidationException("Unsupported issuer '" + issuer + "'. There is no JwtDecoder configured for it.");
        }
    }

}