package ch.admin.bit.jeap.security.resource.configuration;

import ch.admin.bit.jeap.security.resource.authentication.ReactiveSimpleAuthorization;
import ch.admin.bit.jeap.security.resource.log.LoggingBearerTokenServerAccessDeniedHandler;
import ch.admin.bit.jeap.security.resource.log.LoggingBearerTokenServerAuthenticationEntryPoint;
import ch.admin.bit.jeap.security.resource.properties.ResourceServerProperties;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.ReactiveSemanticAuthorization;
import ch.admin.bit.jeap.security.resource.token.AuthoritiesResolver;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationConverter;
import ch.admin.bit.jeap.security.resource.validation.JeapJwtDecoderFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;

import java.util.Optional;

/**
 * Resource server configuration for WebFlux. Check {@link MvcSecurityConfiguration} for the same in MVC
 */
@AutoConfiguration
@Conditional(JeapOAuth2ResourceCondition.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class WebFluxSecurityConfiguration {
    private final ResourceServerProperties resourceServerProperties;
    private final ApplicationContext context;
    private final Environment environment;
    private final AuthoritiesResolver authoritiesResolver;
    private final Optional<JeapMethodSecurityExpressionHandlerCustomizer> expressionHandlerCustomizer;

    @Bean
    public JeapJwtDecoderFactory jeapJwtDecoderFactory() {
        return new JeapJwtDecoderFactory(context, resourceServerProperties);
    }

    @Bean
    public ReactiveSimpleAuthorization jeapSimpleAuthorization() {
        return new ReactiveSimpleAuthorization();
    }

    @Bean
    @Conditional(SemanticAuthorizationCondition.class)
    public ReactiveSemanticAuthorization jeapSemanticAuthorization() {
        return new ReactiveSemanticAuthorization(resourceServerProperties.getSystemName());
    }

    // Custom {@link MethodSecurityExpressionHandler} to support additional checks in security expressions
    @Bean
    @Primary
    public MethodSecurityExpressionHandler customMethodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler;
        if (SemanticAuthorizationCondition.isSemanticAuthorizationEnabled(environment)) {
            // Support semantic role authorization in method security expressions
            expressionHandler = new SemanticMethodSecurityExpressionHandler(resourceServerProperties.getSystemName());
        } else {
            // Support simple role authorization in method security expressions
            expressionHandler = new SimpleMethodSecurityExpressionHandler();
        }
        expressionHandler.setApplicationContext(context);
        return customize(expressionHandler);
    }

    private MethodSecurityExpressionHandler customize(DefaultMethodSecurityExpressionHandler expressionHandler) {
        if (expressionHandlerCustomizer.isPresent()) {
            return expressionHandlerCustomizer.get().customize(expressionHandler);
        } else {
            return expressionHandler;
        }
    }

    @ConditionalOnProperty("jeap.security.oauth2.resourceserver.log.authentication-failure.enabled")
    @ConditionalOnMissingBean(JeapOauth2ResourceServerAuthenticationEntryPoint.class)
    @Bean
    public JeapOauth2ResourceServerAuthenticationEntryPoint jeapOauth2ResourceServerAuthenticationEntryPoint() {
        return new LoggingBearerTokenServerAuthenticationEntryPoint();
    }

    @ConditionalOnProperty("jeap.security.oauth2.resourceserver.log.access-denied.enabled")
    @ConditionalOnMissingBean(JeapOauth2ResourceServerAccessDeniedHandler.class)
    @Bean
    public JeapOauth2ResourceServerAccessDeniedHandler jeapOauth2ResourceServerAccessDeniedHandler(
            @Value("${jeap.security.oauth2.resourceserver.log.access-denied.debug:false}") boolean debugEnabled) {
        return new LoggingBearerTokenServerAccessDeniedHandler(debugEnabled);
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE) //  Allow for overriding
    @SuppressWarnings({"DefaultAnnotationParam", "OptionalUsedAsFieldOrParameterType"})
    public SecurityWebFilterChain oauth2SecurityWebFilterChain(ServerHttpSecurity http,
                                                               JeapJwtDecoderFactory jeapJwtDecoderFactory,
                                                               Optional<JeapOauth2ResourceServerAuthenticationEntryPoint> oauth2ResourceServerAuthenticationEntryPoint,
                                                               Optional<JeapOauth2ResourceServerAccessDeniedHandler> oauth2ResourceServerAccessDeniedHandler) {
        //All requests must be authenticated
        http.authorizeExchange(exchanges -> exchanges.anyExchange().authenticated());

        //Enable CORS
        http.cors(Customizer.withDefaults());

        //Enable CSRF with CookieServerCsrfTokenRepository and make it visible to Angular
        http.csrf(csrf -> csrf.csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse()));

        //Treat endpoints as OAuth2 resources
        ReactiveJwtDecoder jwtDecoder = jeapJwtDecoderFactory.createReactiveJwtDecoder();
        ReactiveJwtAuthenticationConverterAdapter authenticationConverter = new ReactiveJwtAuthenticationConverterAdapter(new JeapAuthenticationConverter(authoritiesResolver));
        http.oauth2ResourceServer(oAuth2ResourceServer -> {
            oAuth2ResourceServer.
                    jwt(jwt -> jwt.
                            jwtDecoder(jwtDecoder).
                            jwtAuthenticationConverter(authenticationConverter));
            oauth2ResourceServerAuthenticationEntryPoint.ifPresent(oAuth2ResourceServer::authenticationEntryPoint);
            oauth2ResourceServerAccessDeniedHandler.ifPresent(oAuth2ResourceServer::accessDeniedHandler);
        });
        return http.build();
    }
}
