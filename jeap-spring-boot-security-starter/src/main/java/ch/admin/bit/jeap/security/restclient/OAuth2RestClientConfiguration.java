package ch.admin.bit.jeap.security.restclient;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
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
import org.springframework.web.client.RestClient;

/**
 * This configuration makes a RestClient builder factory available that can create a RestClient.Builder instance
 * that builds RestClient instances that automatically add an OAuth2 access token as bearer to the RestClient exchanges.
 */
@Slf4j
@AutoConfiguration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ConditionalOnClass(RestClient.class)
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class OAuth2RestClientConfiguration {

    @Configuration
    @Order(Ordered.LOWEST_PRECEDENCE -1) // to be executed before ServletRestClientForNoOAuthClientsConfiguration
    @Conditional(ClientsConfiguredCondition.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
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
    @ConditionalOnMissingBean(JeapOAuth2RestClientBuilderFactory.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class ServletRestClientForNoOAuthClientsConfiguration {
        @Bean
        public JeapOAuth2RestClientBuilderFactory jeapOAuth2RestclientBuilderFactory(RestClient.Builder builder) {
            return new DefaultJeapOAuth2RestClientBuilderFactory(builder, null, null);
        }
    }


}
