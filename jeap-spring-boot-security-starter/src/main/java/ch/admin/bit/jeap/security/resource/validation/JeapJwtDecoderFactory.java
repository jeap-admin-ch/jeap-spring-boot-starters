package ch.admin.bit.jeap.security.resource.validation;

import ch.admin.bit.jeap.security.resource.properties.AuthorizationServerConfiguration;
import ch.admin.bit.jeap.security.resource.properties.ResourceServerProperties;
import ch.admin.bit.jeap.security.resource.validation.IssuerJwtDecoder.IssuerJwtDecoderBuilder;
import ch.admin.bit.jeap.security.resource.validation.ReactiveIssuerJwtDecoder.ReactiveIssuerJwtDecoderBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@RequiredArgsConstructor
public class JeapJwtDecoderFactory {

    private final ApplicationContext applicationContext;
    private final ResourceServerProperties resourceServerProperties;

    /**
     * Create a JWT decoder that accepts tokens from the configured authorization servers.
     *
     * @return The requested JWT decoder
     */
    public JwtDecoder createJwtDecoder() {
        IssuerJwtDecoderBuilder issuerJwtDecoderBuilder = IssuerJwtDecoder.builder();
        DecoderCreator<JwtDecoder> decoderCreator = DecoderCreator::createServletDecoder;
        resourceServerProperties.getAllAuthServerConfigurations().forEach(authConfig -> {
                    var decoder = createDecoder(authConfig, resourceServerProperties.getAudience(), decoderCreator);
                    issuerJwtDecoderBuilder.issuerDecoder(authConfig.getIssuer(), decoder);
                });
        return issuerJwtDecoderBuilder.build();
    }

    /**
     * Create a reactive JWT decoder that accepts tokens from the configured authorization servers.
     *
     * @return The requested JWT decoder
     */
    public ReactiveJwtDecoder createReactiveJwtDecoder() {
        ReactiveIssuerJwtDecoderBuilder issuerJwtDecoderBuilder = ReactiveIssuerJwtDecoder.builder();
        DecoderCreator<ReactiveJwtDecoder> decoderCreator = DecoderCreator::createReactiveDecoder;
        resourceServerProperties.getAllAuthServerConfigurations().forEach(authConfig -> {
                    var decoder = createDecoder(authConfig, resourceServerProperties.getAudience(), decoderCreator);
                    issuerJwtDecoderBuilder.issuerDecoder(authConfig.getIssuer(), decoder);
                });
        return issuerJwtDecoderBuilder.build();
    }

    private <T> T createDecoder(AuthorizationServerConfiguration authServerConfig, String audience, DecoderCreator<T> creator) {
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ofSeconds(30));
        AudienceJwtValidator audienceValidator = new AudienceJwtValidator(audience);
        String issuer = authServerConfig.getIssuer();
        ContextIssuerJwtValidator issuerValidator = new ContextIssuerJwtValidator(authServerConfig.getAuthenticationContexts(), issuer);
        OAuth2TokenValidator<Jwt> jwtValidator = new DelegatingOAuth2TokenValidator<>(timestampValidator, audienceValidator, issuerValidator);
        String jwkSetUri = authServerConfig.getJwkSetUri();
        var claimSetConverter = lookupClaimSetConverter(authServerConfig);
        return creator.create(jwkSetUri, jwtValidator, claimSetConverter);
    }

    @SuppressWarnings("unchecked") // cannot use 'instance of' on parameterized types
    private Converter<Map<String, Object>, Map<String, Object>> lookupClaimSetConverter(AuthorizationServerConfiguration authServerConfig) {
        String claimSetConverterName = authServerConfig.getClaimSetConverterName();
        if (StringUtils.hasText(claimSetConverterName)) {
            try {
                Object bean = applicationContext.getBean(claimSetConverterName);
                return (Converter<Map<String, Object>, Map<String, Object>>) bean;
            } catch (Exception e) {
                throw new IllegalArgumentException("No claim set converter bean with name " + claimSetConverterName + " found in application context.", e);
            }
        } else {
            return MappedJwtClaimSetConverter.withDefaults(Collections.emptyMap());
        }
    }

    private interface DecoderCreator<T> {

        T create(String jwkSetUri, OAuth2TokenValidator<Jwt> jwtValidator, Converter<Map<String, Object>, Map<String, Object>> claimSetConverter);

        static JwtDecoder createServletDecoder(String jwkSetUri, OAuth2TokenValidator<Jwt> jwtValidator, Converter<Map<String, Object>, Map<String, Object>> claimSetConverter) {
            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.
                    withJwkSetUri(jwkSetUri).
                    jwsAlgorithm(SignatureAlgorithm.RS256).
                    jwsAlgorithm(SignatureAlgorithm.RS512).
                    build();
            jwtDecoder.setJwtValidator(jwtValidator);
            jwtDecoder.setClaimSetConverter(claimSetConverter);
            return jwtDecoder;
        }

        static ReactiveJwtDecoder createReactiveDecoder(String jwkSetUri, OAuth2TokenValidator<Jwt> jwtValidator, Converter<Map<String, Object>, Map<String, Object>> claimSetConverter) {
            NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.
                    withJwkSetUri(jwkSetUri).
                    jwsAlgorithm(SignatureAlgorithm.RS256).
                    jwsAlgorithm(SignatureAlgorithm.RS512).
                    build();
            jwtDecoder.setJwtValidator(jwtValidator);
            jwtDecoder.setClaimSetConverter(claimSetConverter);
            return jwtDecoder;
        }
    }
}
