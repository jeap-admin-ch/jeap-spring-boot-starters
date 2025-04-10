package ch.admin.bit.jeap.security.resource.configuration;

import ch.admin.bit.jeap.security.resource.authentication.ServletSimpleAuthorization;
import ch.admin.bit.jeap.security.resource.log.LoggingBearerTokenAccessDeniedHandler;
import ch.admin.bit.jeap.security.resource.log.LoggingBearerTokenAuthenticationEntryPoint;
import ch.admin.bit.jeap.security.resource.log.UserAccessLoggingRequestFilter;
import ch.admin.bit.jeap.security.resource.properties.ResourceServerProperties;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.ServletSemanticAuthorization;
import ch.admin.bit.jeap.security.resource.token.AuthoritiesResolver;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationConverter;
import ch.admin.bit.jeap.security.resource.validation.JeapJwtDecoderFactory;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;

import java.util.Optional;

/**
 * Resource server configuration for MVC. Check {@link WebFluxSecurityConfiguration} for the same in WebFlux
 */
@AutoConfiguration
@Conditional(JeapOAuth2ResourceCondition.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class MvcSecurityConfiguration {
    private final ResourceServerProperties resourceServerProperties;
    private final ApplicationContext context;
    private final Environment environment;
    private final AuthoritiesResolver authoritiesResolver;
    private final Optional<JeapMethodSecurityExpressionHandlerCustomizer> expressionHandlerCustomizer;

    // Custom {@link MethodSecurityExpressionHandler} to support additional checks in security expressions
    @Bean
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

    @Bean
    public JeapJwtDecoderFactory jeapJwtDecoderFactory() {
        return new JeapJwtDecoderFactory(context, resourceServerProperties);
    }

    @Bean
    public ServletSimpleAuthorization jeapSimpleAuthorization() {
        return new ServletSimpleAuthorization();
    }

    @Bean
    @Conditional(SemanticAuthorizationCondition.class)
    public ServletSemanticAuthorization jeapSemanticAuthorization() {
        return new ServletSemanticAuthorization(resourceServerProperties.getSystemName());
    }

    @ConditionalOnProperty("jeap.security.oauth2.resourceserver.log.authentication-failure.enabled")
    @ConditionalOnMissingBean(JeapOauth2ResourceAuthenticationEntryPoint.class)
    @Bean
    public JeapOauth2ResourceAuthenticationEntryPoint jeapOauth2ResourceAuthenticationEntryPoint() {
        return new LoggingBearerTokenAuthenticationEntryPoint();
    }

    @ConditionalOnProperty("jeap.security.oauth2.resourceserver.log.access-denied.enabled")
    @ConditionalOnMissingBean(JeapOauth2ResourceAccessDeniedHandler.class)
    @Bean
    public JeapOauth2ResourceAccessDeniedHandler jeapOauth2ResourceAccessDeniedHandler(
            @Value("${jeap.security.oauth2.resourceserver.log.access-denied.debug:false}") boolean debugEnabled) {
        return new LoggingBearerTokenAccessDeniedHandler(debugEnabled);
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE) //  Allow for overriding
    @SuppressWarnings({"DefaultAnnotationParam", "OptionalUsedAsFieldOrParameterType"})
    public SecurityFilterChain oauth2SecurityWebFilterChain(HttpSecurity http,
                                                            JeapJwtDecoderFactory jeapJwtDecoderFactory,
                                                            Optional<JeapOauth2ResourceAuthenticationEntryPoint> jeapOauth2ResourceAuthenticationEntryPoint,
                                                            Optional<JeapOauth2ResourceAccessDeniedHandler> jeapOauth2ResourceAccessDeniedHandler,
                                                            @Value("${server.error.path:${error.path:/error}}") String errorPath) throws Exception {

        //All requests must be authenticated except internal error dispatches to the error page
        http.authorizeHttpRequests(authorizeHttpRequests ->
                authorizeHttpRequests.
                        requestMatchers(
                                RequestMatchers.allOf(
                                        AntPathRequestMatcher.antMatcher(HttpMethod.GET, errorPath),
                                        new DispatcherTypeRequestMatcher(DispatcherType.ERROR))
                        ).permitAll().
                        anyRequest().fullyAuthenticated()
        );

        //Enable CORS
        http.cors(Customizer.withDefaults());

        //Enable CSRF with CookieCsrfTokenRepository as can be used from Angular
        http.csrf(csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));

        //No session management is needed, we want stateless
        http.sessionManagement(sessionManagement ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        //Treat endpoints as OAuth2 resources
        JwtDecoder jwtDecoder = jeapJwtDecoderFactory.createJwtDecoder();
        JeapAuthenticationConverter authenticationConverter = new JeapAuthenticationConverter(authoritiesResolver);
        http.oauth2ResourceServer(oauth2ResourceServer -> {
            oauth2ResourceServer
                    .jwt(jwt -> jwt
                            .decoder(jwtDecoder).
                            jwtAuthenticationConverter(authenticationConverter));
            jeapOauth2ResourceAuthenticationEntryPoint.ifPresent(oauth2ResourceServer::authenticationEntryPoint);
            jeapOauth2ResourceAccessDeniedHandler.ifPresent(oauth2ResourceServer::accessDeniedHandler);
        });
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "jeap.security.oauth2.resourceserver.log-user-access", havingValue = "true")
    UserAccessLoggingRequestFilter userAccessLoggingRequestFilter() {
        return new UserAccessLoggingRequestFilter();
    }

}
