package ch.admin.bit.jeap.swagger;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.stream.Collectors;

/**
 * This will secure the swagger UI with base auth. As it has order highest
 * precedence it will ignore any other security configuration for this endpoint
 * If this behavior is not desired, set property jeap.swagger.status to DISABLE or OPEN
 */
@AutoConfiguration
@ConditionalOnClass(WebSecurityConfiguration.class)
@ConditionalOnExpression("'${jeap.swagger.status}' != 'CUSTOM'")
class SwaggerWebSecurityConfig {
    private final static String SWAGGER_ROLE = "swagger";

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @RequiredArgsConstructor
    public static class SwaggerWebmvcSecurity {
        private final SwaggerProperties swaggerProperties;

        @Order(Ordered.HIGHEST_PRECEDENCE + 8)
        @Bean
        public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
            //Only responsible for swagger stuff
            http.securityMatcher(swaggerRequestMatcher());

            //Depending on swagger status set different config
            switch (swaggerProperties.getStatus()) {
                case OPEN:
                    http.authorizeHttpRequests().anyRequest().permitAll();
                    break;
                case SECURED:
                    http.authorizeHttpRequests().anyRequest().hasRole(SWAGGER_ROLE);
                    http.httpBasic();
                    http.authenticationManager(createSwaggerAuth(http.getSharedObject(AuthenticationManagerBuilder.class)));
                    break;
                case DISABLED:
                    http.authorizeHttpRequests().anyRequest().denyAll();
                    break;
                default:
                    throw new IllegalArgumentException(swaggerProperties.getStatus() + " is not a valid value");
            }

            return http.build();
        }

        private AuthenticationManager createSwaggerAuth(AuthenticationManagerBuilder auth) throws Exception {
            SwaggerProperties.Secured secured = swaggerProperties.getSecured();
            if (secured.getUsername() == null || secured.getPassword() == null) {
                throw new IllegalArgumentException("Swagger is set to secured but username or password is not set");
            }
            auth.inMemoryAuthentication()
                    .withUser(secured.getUsername()).password(secured.getPassword())
                    .roles(SWAGGER_ROLE);
            return auth.build();
        }

        private RequestMatcher swaggerRequestMatcher() {
            return new OrRequestMatcher(swaggerProperties.getAntPathPatters().stream()
                    .map(pattern -> new AntPathRequestMatcher(pattern, "GET"))
                    .collect(Collectors.toList()));
        }
    }

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @RequiredArgsConstructor
    public static class SwaggerWebfluxSecurity {
        private final SwaggerProperties swaggerProperties;

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE + 8)
        public SecurityWebFilterChain swaggerSecurityWebFilterChain(ServerHttpSecurity http) {
            //Only responsible for swagger stuff
            http.securityMatcher(swaggerRequestMatcher());

            //Depending on swagger status set different config
            switch (swaggerProperties.getStatus()) {
                case OPEN:
                    http.authorizeExchange().anyExchange().permitAll();
                    break;
                case SECURED:
                    http.authorizeExchange().anyExchange().hasRole(SWAGGER_ROLE);
                    http.httpBasic(this::setAuthenticationManager);
                    break;
                case DISABLED:
                    http.authorizeExchange().anyExchange().denyAll();
                    break;
                default:
                    throw new IllegalArgumentException(swaggerProperties.getStatus() + " is not a valid value");
            }
            return http.build();
        }

        private ServerWebExchangeMatcher swaggerRequestMatcher() {
            return new OrServerWebExchangeMatcher(swaggerProperties.getAntPathPatters().stream()
                    .map(pattern -> new PathPatternParserServerWebExchangeMatcher(pattern, HttpMethod.GET))
                    .collect(Collectors.toList()));
        }

        private void setAuthenticationManager(ServerHttpSecurity.HttpBasicSpec basicSpec) {
            if (swaggerProperties.getStatus() != SwaggerProperties.SwaggerStatus.SECURED) {
                return;
            }
            SwaggerProperties.Secured secured = swaggerProperties.getSecured();

            if (secured.getUsername() == null || secured.getPassword() == null) {
                throw new IllegalArgumentException("Swagger is set to secured but username or password is not set");
            }

            basicSpec.authenticationManager(new UserDetailsRepositoryReactiveAuthenticationManager(
                    new MapReactiveUserDetailsService(
                            User.withUsername(secured.getUsername()).password(secured.getPassword())
                                    .roles(SWAGGER_ROLE).build())));
        }
    }
}

