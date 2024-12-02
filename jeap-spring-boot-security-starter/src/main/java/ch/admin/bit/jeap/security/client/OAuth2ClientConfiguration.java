package ch.admin.bit.jeap.security.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.server.resource.web.reactive.function.client.ServerBearerExchangeFilterFunction;
import org.springframework.security.oauth2.server.resource.web.reactive.function.client.ServletBearerExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * This configuration makes a WebClient builder factory available that can create a WebClient.Builder instance
 * that builds WebClient instances that automatically add an OAuth2 access token as bearer to the WebClient exchanges.
 *
 * This configuration supports both the WebMvc and the WebFlux stack.
 */

@Slf4j
@AutoConfiguration
@ConditionalOnClass(WebClient.class)
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class OAuth2ClientConfiguration {

    @Configuration
    @Order(Ordered.LOWEST_PRECEDENCE -1) // to be executed before ServletWebClientForNoOAuthClientsConfiguration
    @Conditional(ClientsConfiguredCondition.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class ServletWebClientForOAuthClientsConfiguration {
        @Bean
        public JeapOAuth2WebclientBuilderFactory jeapOAuth2WebclientBuilderFactory(WebClient.Builder builder, ClientRegistrationRepository clientRegistrationRepository, OAuth2AuthorizedClientService clientService) {
            return new DefaultJeapOAuth2WebclientBuilderFactory(builder,
                    new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager(clientRegistrationRepository, clientService)),
                    new ServletBearerExchangeFilterFunction(), clientRegistrationRepository::findByRegistrationId);
        }

        private OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clientRegistrationRepository, OAuth2AuthorizedClientService clientService) {
            AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, clientService);
            OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build();
            authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
            return authorizedClientManager;
        }

    }

    @Configuration
    @ConditionalOnMissingBean(JeapOAuth2WebclientBuilderFactory.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class ServletWebClientForNoOAuthClientsConfiguration {
        @Bean
        public JeapOAuth2WebclientBuilderFactory jeapOAuth2WebclientBuilderFactory(WebClient.Builder builder) {
            return new DefaultJeapOAuth2WebclientBuilderFactory(builder, new ServletBearerExchangeFilterFunction());
        }
    }

    @Configuration
    @Order(Ordered.LOWEST_PRECEDENCE -1) // to be executed before ReactiveWebClientForNoOAuthClientsConfiguration
    @Conditional(ClientsConfiguredCondition.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public static class ReactivetWebClientForOAuthClientsConfiguration {
        @Bean
        public JeapOAuth2WebclientBuilderFactory jeapOAuth2WebclientBuilderFactory(WebClient.Builder builder, ReactiveClientRegistrationRepository clientRegistrationRepository, ReactiveOAuth2AuthorizedClientService clientService) {
            return new DefaultJeapOAuth2WebclientBuilderFactory(builder,
                    new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager(clientRegistrationRepository, clientService)),
                    new ServerBearerExchangeFilterFunction(), id -> clientRegistrationRepository.findByRegistrationId(id).block());
        }

        private AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager(ReactiveClientRegistrationRepository clientRegistrationRepository, ReactiveOAuth2AuthorizedClientService clientService) {
            AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, clientService);
            ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build();
            authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
            return authorizedClientManager;
        }
    }

    @Configuration
    @ConditionalOnMissingBean(JeapOAuth2WebclientBuilderFactory.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public static class ReactivetWebClientForNoOAuthClientsConfiguration {
        @Bean
        public JeapOAuth2WebclientBuilderFactory jeapOAuth2WebclientBuilderFactory(WebClient.Builder builder) {
            return new DefaultJeapOAuth2WebclientBuilderFactory(builder, new ServerBearerExchangeFilterFunction());
        }
    }

}
