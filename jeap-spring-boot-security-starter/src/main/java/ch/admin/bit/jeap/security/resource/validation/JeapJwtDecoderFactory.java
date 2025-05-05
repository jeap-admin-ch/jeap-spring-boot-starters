package ch.admin.bit.jeap.security.resource.validation;

import ch.admin.bit.jeap.security.resource.introspection.JeapJwtIntrospection;
import ch.admin.bit.jeap.security.resource.properties.AuthorizationServerConfiguration;
import ch.admin.bit.jeap.security.resource.properties.ResourceServerProperties;
import ch.admin.bit.jeap.security.resource.validation.IssuerJwtDecoder.IssuerJwtDecoderBuilder;
import ch.admin.bit.jeap.security.resource.validation.ReactiveIssuerJwtDecoder.ReactiveIssuerJwtDecoderBuilder;
import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@RequiredArgsConstructor
public class JeapJwtDecoderFactory {

    private final ApplicationContext applicationContext;
    private final ResourceServerProperties resourceServerProperties;
    private final JeapJwtIntrospection jeapJwtIntrospection;

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
        JwtDecoder issuerJwtDecoder = issuerJwtDecoderBuilder.build();
        return addIntrospectionIfConfigured(issuerJwtDecoder);
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
        return creator.create(jwkSetUri, jwtValidator, claimSetConverter, new JwksTimeoutConfiguration(
                        authServerConfig.getJwksConnectTimeoutInMillis(),
                        authServerConfig.getJwksReadTimeoutInMillis()
                )
        );
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

    private JwtDecoder addIntrospectionIfConfigured(JwtDecoder jwtDecoder) {
        if (jeapJwtIntrospection != null) {
            return new IntrospectingJwtDecoder(jwtDecoder, jeapJwtIntrospection);
        } else {
            return jwtDecoder;
        }
    }

    interface DecoderCreator<T> {

        T create(String jwkSetUri, OAuth2TokenValidator<Jwt> jwtValidator, Converter<Map<String, Object>, Map<String, Object>> claimSetConverter, JwksTimeoutConfiguration jwksTimeoutConfiguration);

        static JwtDecoder createServletDecoder(String jwkSetUri, OAuth2TokenValidator<Jwt> jwtValidator, Converter<Map<String, Object>, Map<String, Object>> claimSetConverter, JwksTimeoutConfiguration jwksTimeoutConfiguration) {
            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                    .withJwkSetUri(jwkSetUri)
                    .jwsAlgorithm(SignatureAlgorithm.RS256)
                    .jwsAlgorithm(SignatureAlgorithm.RS512)
                    .restOperations(createRestTemplate(jwksTimeoutConfiguration))
                    .build();
            jwtDecoder.setJwtValidator(jwtValidator);
            jwtDecoder.setClaimSetConverter(claimSetConverter);
            return jwtDecoder;
        }

        static ReactiveJwtDecoder createReactiveDecoder(String jwkSetUri, OAuth2TokenValidator<Jwt> jwtValidator, Converter<Map<String, Object>, Map<String, Object>> claimSetConverter, JwksTimeoutConfiguration jwksTimeoutConfiguration) {
            NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder
                    .withJwkSetUri(jwkSetUri)
                    .jwsAlgorithm(SignatureAlgorithm.RS256)
                    .jwsAlgorithm(SignatureAlgorithm.RS512)
                    .webClient(createWebClient(jwksTimeoutConfiguration))
                    .build();
            jwtDecoder.setJwtValidator(jwtValidator);
            jwtDecoder.setClaimSetConverter(claimSetConverter);
            return jwtDecoder;
        }

        private static RestTemplate createRestTemplate(JwksTimeoutConfiguration jwksTimeoutConfiguration) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(jwksTimeoutConfiguration.connectTimeoutInMillis());
            requestFactory.setReadTimeout(jwksTimeoutConfiguration.readTimeoutInMillis());
            return new RestTemplate(requestFactory);
        }

        private static WebClient createWebClient(JwksTimeoutConfiguration jwksTimeoutConfiguration) {
            HttpClient httpClient = HttpClient.create()
                    .responseTimeout(Duration.ofMillis(jwksTimeoutConfiguration.readTimeoutInMillis()))
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, jwksTimeoutConfiguration.connectTimeoutInMillis());
            return WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();
        }
    }

    record JwksTimeoutConfiguration(
            int connectTimeoutInMillis,
            int readTimeoutInMillis) {
    }
}
