package ch.admin.bit.jeap.security.test.client.configuration;

import ch.admin.bit.jeap.security.test.client.MockJeapOAuth2RestClientBuilderFactory;
import ch.admin.bit.jeap.security.test.client.MockJeapOAuth2WebclientBuilderFactory;
import ch.admin.bit.jeap.security.test.jws.configuration.JwsTestSupportConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Import({DisableJeapOAuth2ClientConfiguration.class, JwsTestSupportConfiguration.class})
@ImportAutoConfiguration(exclude = OAuth2ClientAutoConfiguration.class)
public class JeapOAuth2IntegrationTestClientConfiguration {


    @ConditionalOnClass(RestClient.class)
    public static class RestClientIntegrationTestConfiguration {
        @Bean
        public MockJeapOAuth2RestClientBuilderFactory jeapOAuth2RestClientBuilderFactory(RestClient.Builder restClientBuilder) {
            return new MockJeapOAuth2RestClientBuilderFactory(restClientBuilder);
        }
    }

    @ConditionalOnClass(WebClient.class)
    public static class WebClientIntegrationTestConfiguration {
        @Bean
        public MockJeapOAuth2WebclientBuilderFactory jeapOAuth2WebclientBuilderFactory(WebClient.Builder webClientBuilder) {
            return new MockJeapOAuth2WebclientBuilderFactory(webClientBuilder);
        }
    }

}
