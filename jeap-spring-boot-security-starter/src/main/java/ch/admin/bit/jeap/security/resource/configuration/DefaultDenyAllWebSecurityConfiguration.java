package ch.admin.bit.jeap.security.resource.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;

/**
 * If the security starter has been added as a dependency but the OAuth2 secured web configuration has not been
 * activated e.g. because the needed configuration properties are missing, then deny access to all web endpoints as a
 * secure default web security configuration.
 */

@Slf4j
@AutoConfiguration(after = {MvcSecurityConfiguration.class, WebFluxSecurityConfiguration.class})
@SuppressWarnings({"DefaultAnnotationParam"})
public class DefaultDenyAllWebSecurityConfiguration {
    private static final String DENY_ALL_MESSAGE = "jeap-spring-boot-security-starter did not activate OAuth2 resource security " +
            "for web endpoints. Activating a 'deny-all' configuration as secure fallback. " +
            "Override the 'deny-all' configuration with your own web security configuration " +
            "or define the configuration properties needed for the OAuth2 resource security.";

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(MvcSecurityConfiguration.class)
    @EnableWebSecurity
    public static class WebMvcDenyAllWebSecurityConfiguration {
        @Bean
        @Order(Ordered.LOWEST_PRECEDENCE) //  Allow for overriding
        public SecurityFilterChain oauth2SecurityWebFilterChain(HttpSecurity http) throws Exception {
            log.debug(DENY_ALL_MESSAGE);
            http.
                    authorizeHttpRequests(authorizeHttpRequests ->
                            authorizeHttpRequests.anyRequest().denyAll()).
                    exceptionHandling(exceptionHandling->
                            exceptionHandling.authenticationEntryPoint((new HttpStatusEntryPoint(HttpStatus.FORBIDDEN))));
            return http.build();
        }
    }

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnMissingBean(WebFluxSecurityConfiguration.class)
    @EnableWebFluxSecurity
    public static class WebFluxDenyAllWebSecurityConfiguration {
        @Bean
        @Order(Ordered.LOWEST_PRECEDENCE)  //  Allow for overriding
        public SecurityWebFilterChain denyAllSecurityWebFilterChain(ServerHttpSecurity http) {
            log.debug(DENY_ALL_MESSAGE);
            http.authorizeExchange(exchanges ->
                        exchanges.anyExchange().denyAll()).
                exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.FORBIDDEN)));
            return http.build();
        }
    }

}
