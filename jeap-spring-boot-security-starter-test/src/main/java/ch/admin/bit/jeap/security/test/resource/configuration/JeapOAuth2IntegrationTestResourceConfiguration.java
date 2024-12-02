package ch.admin.bit.jeap.security.test.resource.configuration;

import ch.admin.bit.jeap.security.test.jws.configuration.JwsTestSupportConfiguration;
import ch.admin.bit.jeap.security.test.jws.TestKeyProvider;
import ch.admin.bit.jeap.security.test.resource.jwks.ReactiveJwksEndpointMock;
import ch.admin.bit.jeap.security.test.resource.jwks.ServletJwksEndpointMock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

import java.util.Collections;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@Import({DisableJeapPermitAllSecurityConfiguration.class, JwsTestSupportConfiguration.class})
public class JeapOAuth2IntegrationTestResourceConfiguration {

    public static final int JWKS_ENDPOINT_MOCK_SECURITY_CONFIGURATION_PRECEDENCE = Ordered.HIGHEST_PRECEDENCE+5;

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class ServletOAuth2IntegrationTestConfiguration {

        @Bean
        public ServletJwksEndpointMock jwksEndpointMock(TestKeyProvider testKeyProvider) {
            return new ServletJwksEndpointMock(Collections.singleton(testKeyProvider.getAuthServerKey()));
        }

        @Bean
        @Order(JWKS_ENDPOINT_MOCK_SECURITY_CONFIGURATION_PRECEDENCE)
        public SecurityFilterChain servletJwksSecurityFilterChain(HttpSecurity http) throws Exception {
            http.
                    securityMatchers(matchers ->
                            matchers.requestMatchers(antMatcher(ServletJwksEndpointMock.getContextPath() + "/**"))).
                    authorizeHttpRequests(authorizeHttpRequests ->
                            authorizeHttpRequests.anyRequest().permitAll());
            return http.build();
        }

    }

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public static class ReactiveOAuth2IntegrationTestConfiguration {

        @Bean
        public ReactiveJwksEndpointMock jwksEndpointMock(TestKeyProvider testKeyProvider) {
            return new ReactiveJwksEndpointMock(Collections.singleton(testKeyProvider.getAuthServerKey()));
        }

        @Bean
        @Order(JWKS_ENDPOINT_MOCK_SECURITY_CONFIGURATION_PRECEDENCE)
        public SecurityWebFilterChain jwksEndpointMockSecurityFilterChain(ServerHttpSecurity http) {
            http.
                    securityMatcher(ServerWebExchangeMatchers.pathMatchers(ReactiveJwksEndpointMock.getContextPath() + "/**")).
                    authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
            return http.build();
        }

    }

}
