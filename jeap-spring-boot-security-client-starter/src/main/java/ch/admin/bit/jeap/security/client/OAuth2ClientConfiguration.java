package ch.admin.bit.jeap.security.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * This configuration makes a WebClient builder factory available that can create a WebClient.Builder instance
 * that builds WebClient instances that automatically add an OAuth2 access token as bearer to the WebClient exchanges.
 * <p>
 * This configuration supports both the WebMvc and the WebFlux stack.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnBean(WebClient.Builder.class)
@AutoConfigureAfter({OAuth2ClientAutoConfiguration.class, ReactiveOAuth2ClientAutoConfiguration.class, WebClientAutoConfiguration.class})
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class OAuth2ClientConfiguration {

    @Configuration
    @ConditionalOnBean({ClientRegistrationRepository.class, OAuth2AuthorizedClientService.class})
    @ConditionalOnMissingBean(JeapOAuth2WebclientBuilderFactory.class)
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
    @ConditionalOnMissingBean({ClientRegistrationRepository.class, OAuth2AuthorizedClientService.class, JeapOAuth2WebclientBuilderFactory.class})
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class ServletWebClientForNoOAuthClientsConfiguration {
        @Bean
        public JeapOAuth2WebclientBuilderFactory jeapOAuth2WebclientBuilderFactory(WebClient.Builder builder) {
            return new DefaultJeapOAuth2WebclientBuilderFactory(builder, new ServletBearerExchangeFilterFunction());
        }
    }

    @Configuration
    @ConditionalOnBean({ReactiveClientRegistrationRepository.class, ReactiveOAuth2AuthorizedClientService.class})
    @ConditionalOnMissingBean(JeapOAuth2WebclientBuilderFactory.class)
    public static class ReactiveWebClientForOAuthClientsConfiguration {
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
    @ConditionalOnMissingBean({ReactiveClientRegistrationRepository.class, ReactiveOAuth2AuthorizedClientService.class, JeapOAuth2WebclientBuilderFactory.class})
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public static class ReactiveWebClientForNoOAuthClientsConfiguration {
        @Bean
        public JeapOAuth2WebclientBuilderFactory jeapOAuth2WebclientBuilderFactory(WebClient.Builder builder) {
            return new DefaultJeapOAuth2WebclientBuilderFactory(builder, new ServerBearerExchangeFilterFunction());
        }
    }

}
