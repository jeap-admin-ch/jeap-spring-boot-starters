package ch.admin.bit.jeap.monitor;

import ch.admin.bit.jeap.monitor.MonitoringConfig.ActuatorConfig;
import ch.admin.bit.jeap.monitor.MonitoringConfig.PrometheusConfig;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.EndpointRequestMatcher;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.AuthorizeExchangeSpec;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;


/**
 * <p>
 * This secures the prometheus endpoint with basic auth. As it has order highest precedence it will
 * ignore any other security configuration for this endpoint.
 * If this behavior is not desired, set a property jeap.monitor.prometheus.secure=false to enable public access to the
 * prometheus endpoint.
 * </p>
 * <p>
 * Also disables all actuator endpoints by default, except for info and health. Set jeap.actuator.enable-admin-endpoints=true
 * to enable default endpoints used by Spring Boot Admin to be enabled, and configure the credentials for basic auth
 * access from Boot Admin using jeap.monitor.actuator.user/password.
 * </p>
 */
@AutoConfiguration
public class ActuatorSecurity {

    public static final String PROMETHEUS_ROLE = "PROMETHEUS";
    public static final String ACTUATOR_ROLE = "ACTUATOR";

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class ActuatorWebmvcSecurity {

        private static final RequestMatcher[] NO_MATCHERS = new RequestMatcher[0];

        private final PrometheusConfig prometheusConfig;
        private final ActuatorConfig actuatorConfig;
        private final String actuatorOverviewPagePath;

        public ActuatorWebmvcSecurity(MonitoringConfig monitoringConfig, WebEndpointProperties webEndpointProperties) {
            this.prometheusConfig = monitoringConfig.getPrometheus();
            this.actuatorConfig = monitoringConfig.getActuator();
            this.actuatorOverviewPagePath = webEndpointProperties.getBasePath();
        }

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE + 9)
        public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
            http.
                    securityMatcher(org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.toAnyEndpoint()).
                    authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.
                            requestMatchers(openEndpoints()).permitAll().
                            requestMatchers(securedPrometheusMatcher()).hasRole(PROMETHEUS_ROLE).
                            requestMatchers(antMatcher(HttpMethod.GET, actuatorOverviewPagePath)).hasRole(ACTUATOR_ROLE).
                            requestMatchers(adminEndpointMatcher()).hasRole(ACTUATOR_ROLE).
                            requestMatchers(additionalAdminEndpointMatcher()).hasRole(ACTUATOR_ROLE).
                            anyRequest().denyAll()).
                    csrf(csrf -> csrf.disable()).
                    httpBasic(httpBasic -> httpBasic.
                            authenticationEntryPoint(new ActuatorBasicAuthenticationEntryPoint())).
                    exceptionHandling(exceptionHandling ->
                            exceptionHandling.accessDeniedHandler(new ActuatorAccessDeniedHandler())).
                    authenticationManager(createServletActuatorAuthManager(http.getSharedObject(AuthenticationManagerBuilder.class)));
            return http.build();
        }

        private AuthenticationManager createServletActuatorAuthManager(AuthenticationManagerBuilder auth) throws Exception {
            auth.inMemoryAuthentication()
                    .passwordEncoder(NoUpgradeEncodingDelegatingPasswordEncoder.createInstance())
                    .withUser(prometheusConfig.getUser()).password(prometheusConfig.getPassword()).roles(PROMETHEUS_ROLE).and()
                    .withUser(actuatorConfig.getUser()).password(actuatorConfig.getPassword()).roles(ACTUATOR_ROLE);
            return auth.build();
        }

        private RequestMatcher[] adminEndpointMatcher() {
            return actuatorConfig.isEnableAdminEndpoints() ? toMatchers(actuatorConfig.getPermittedEndpoints()) : NO_MATCHERS;
        }

        private RequestMatcher[] additionalAdminEndpointMatcher() {
            return actuatorConfig.isEnableAdminEndpoints() ? toMatchers(actuatorConfig.getAdditionalPermittedEndpoints()) : NO_MATCHERS;
        }

        private RequestMatcher[] securedPrometheusMatcher() {
            return prometheusConfig.isSecure() ? toMatchers(PrometheusScrapeEndpoint.class) : NO_MATCHERS;
        }

        private RequestMatcher[] openEndpoints() {
            if (!prometheusConfig.isSecure()) {
                return toMatchers(HealthEndpoint.class, InfoEndpoint.class, PrometheusScrapeEndpoint.class);
            }
            return toMatchers(HealthEndpoint.class, InfoEndpoint.class);
        }

        private RequestMatcher[] toMatchers(Class<?>... endpoints) {
            return Arrays.stream(endpoints)
                    .map(this::toRequestMatcher)
                    .toArray(RequestMatcher[]::new);
        }

        private RequestMatcher toRequestMatcher(Class<?> endpoint) {
            EndpointRequestMatcher endpointRequestMatcher = EndpointRequest.to(endpoint);
            // If admin endpoints are enabled match requests for changing log levels in addition to requests for reading log levels
            if (actuatorConfig.isEnableAdminEndpoints() && ActuatorEndpointIdUtil.isLoggersEndpoint(endpoint)) {
                return HttpMethodFilteringRequestMatcher.filterGetAndPost(endpointRequestMatcher);
            } else {
                // Per default match GET requests only
                return HttpMethodFilteringRequestMatcher.filterGet(endpointRequestMatcher);
            }
        }
    }

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public static class ActuatorWebfluxSecurity {

        private final PrometheusConfig prometheusConfig;
        private final ActuatorConfig actuatorConfig;
        private final String actuatorOverviewPagePath;

        public ActuatorWebfluxSecurity(MonitoringConfig monitoringConfig, WebEndpointProperties webEndpointProperties) {
            this.prometheusConfig = monitoringConfig.getPrometheus();
            this.actuatorConfig = monitoringConfig.getActuator();
            this.actuatorOverviewPagePath = webEndpointProperties.getBasePath();
        }

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE + 9)
        public SecurityWebFilterChain actuatorSecurityWebFilterChain(ServerHttpSecurity http) {
            http.
                    securityMatcher(org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest.toAnyEndpoint()).
                    httpBasic(httpBasic -> httpBasic.authenticationManager(createReactivePrometheusAuthenticationManager())).
                    csrf(csrf -> csrf.disable()).
                    authorizeExchange(exchanges -> {
                        exchanges.
                                matchers(openEndpoints()).permitAll().
                                pathMatchers(HttpMethod.GET, actuatorOverviewPagePath).hasRole(ACTUATOR_ROLE);
                        if (actuatorConfig.isEnableAdminEndpoints()) {
                            configureAdminEndpoints(exchanges);
                        }
                        if (prometheusConfig.isSecure()) {
                            exchanges.matchers(toMatchers(PrometheusScrapeEndpoint.class)).hasRole(PROMETHEUS_ROLE);
                        }
                        exchanges.anyExchange().denyAll();
                    });
            return http.build();
        }

        private void configureAdminEndpoints(AuthorizeExchangeSpec authorizeExchangeSpec) {
            authorizeExchangeSpec.matchers(toMatchers(actuatorConfig.getPermittedEndpoints())).hasRole(ACTUATOR_ROLE);
            if (actuatorConfig.getAdditionalPermittedEndpoints() != null && actuatorConfig.getAdditionalPermittedEndpoints().length > 0) {
                authorizeExchangeSpec.matchers(toMatchers(actuatorConfig.getAdditionalPermittedEndpoints())).hasRole(ACTUATOR_ROLE);
            }
        }

        private ReactiveAuthenticationManager createReactivePrometheusAuthenticationManager() {
            return new UserDetailsRepositoryReactiveAuthenticationManager(
                    new MapReactiveUserDetailsService(
                            User.withUsername(prometheusConfig.getUser()).password(prometheusConfig.getPassword()).roles(PROMETHEUS_ROLE).build(),
                            User.withUsername(actuatorConfig.getUser()).password(actuatorConfig.getPassword()).roles(ACTUATOR_ROLE).build()));
        }

        private ServerWebExchangeMatcher[] openEndpoints() {
            if (!prometheusConfig.isSecure()) {
                return toMatchers(HealthEndpoint.class, InfoEndpoint.class, PrometheusScrapeEndpoint.class);
            }
            return toMatchers(HealthEndpoint.class, InfoEndpoint.class);
        }


        private ServerWebExchangeMatcher[] toMatchers(Class<?>... endpoints) {
            return Arrays.stream(endpoints)
                    .map(this::toServerWebExchangeMatcher)
                    .toArray(ServerWebExchangeMatcher[]::new);
        }

        private ServerWebExchangeMatcher toServerWebExchangeMatcher(Class<?> endpoint) {
            ServerWebExchangeMatcher endpointServerWebExchangeMatcher = org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest.to(endpoint);
            // If admin endpoints are enabled match requests for changing log levels in addition to requests for reading log levels
            if (actuatorConfig.isEnableAdminEndpoints() && ActuatorEndpointIdUtil.isLoggersEndpoint(endpoint)) {
                return HttpMethodFilteringServerWebExchangeMatcher.filterGetAndPost(endpointServerWebExchangeMatcher);
            } else {
                // Per default match GET requests only
                return HttpMethodFilteringServerWebExchangeMatcher.filterGet(endpointServerWebExchangeMatcher);
            }
        }

    }
}
