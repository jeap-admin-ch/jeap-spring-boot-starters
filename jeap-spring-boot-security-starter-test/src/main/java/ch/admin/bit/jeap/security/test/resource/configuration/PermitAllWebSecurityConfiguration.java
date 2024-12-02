package ch.admin.bit.jeap.security.test.resource.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;

@AutoConfiguration
public class PermitAllWebSecurityConfiguration {

    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @Configuration
    public static class ServletPermitAllWebSecurityConfiguration {
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE + 5) // Overrule other web security configurations, but still allow room for overriding.
        public SecurityFilterChain servletPermitAllSecurityFilterChain(HttpSecurity http) throws Exception {
            http.
                    csrf(csrf -> csrf.disable()).
                    authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.anyRequest().permitAll());
            return http.build();
        }
    }

    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @Configuration
    public static class ReactivePermitAllWebSecurityConfiguration {
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE + 5)  // Overrule other web security configurations, but still allow room for overriding.
        public SecurityWebFilterChain permitAllSecurityWebFilterChain(ServerHttpSecurity http) {
            http.
                    csrf(csrf -> csrf.disable()).
                    authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
            return http.build();
        }
    }

}
