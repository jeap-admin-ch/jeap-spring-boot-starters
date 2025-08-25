package ch.admin.bit.jeap.security.restclient;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

/**
 * This configuration makes a RestClient builder factory available that can create a RestClient.Builder instance
 * that builds RestClient instances that automatically add an OAuth2 access token as a bearer token to the RestClient exchanges.
 */
@Slf4j
@AutoConfiguration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ConditionalOnBean(RestClient.Builder.class)
@AutoConfigureAfter(RestClientAutoConfiguration.class)
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class OAuth2RestClientConfiguration {

    @Configuration
    @ConditionalOnBean({ClientRegistrationRepository.class, OAuth2AuthorizedClientService.class})
    public static class ServletRestClientForOAuthClientsConfiguration {
        @Bean
        public JeapOAuth2RestClientBuilderFactory jeapOAuth2RestclientBuilderFactory(RestClient.Builder builder, ClientRegistrationRepository clientRegistrationRepository, OAuth2AuthorizedClientService clientService) {
            return new DefaultJeapOAuth2RestClientBuilderFactory(builder,
                    authorizedClientManager(clientRegistrationRepository, clientService),
                    clientRegistrationRepository);
        }

        private OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clientRegistrationRepository, OAuth2AuthorizedClientService clientService) {
            AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, clientService);
            OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build();
            authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
            return authorizedClientManager;
        }
    }

    @Configuration
    @ConditionalOnMissingBean({ClientRegistrationRepository.class, OAuth2AuthorizedClientService.class, JeapOAuth2RestClientBuilderFactory.class})
    public static class ServletRestClientForNoOAuthClientsConfiguration {
        @Bean
        public JeapOAuth2RestClientBuilderFactory jeapOAuth2RestClientBuilderFactory(RestClient.Builder builder) {
            return new DefaultJeapOAuth2RestClientBuilderFactory(builder, null, null);
        }
    }

}
