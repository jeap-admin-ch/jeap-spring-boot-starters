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
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
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
                    http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
                    break;
                case SECURED:
                    http.authorizeHttpRequests(auth -> auth.anyRequest().hasRole(SWAGGER_ROLE));
                    http.httpBasic(Customizer.withDefaults());
                    http.authenticationManager(createSwaggerAuth(http.getSharedObject(AuthenticationManagerBuilder.class)));
                    break;
                case DISABLED:
                    http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
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
            var builder = PathPatternRequestMatcher.withDefaults();
            return new OrRequestMatcher(swaggerProperties.getAntPathPatters().stream()
                    .map(pattern -> builder.matcher(HttpMethod.GET, pattern))
                    .collect(Collectors.toList()));
        }
    }
}

