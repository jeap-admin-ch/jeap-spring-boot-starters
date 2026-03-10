package ch.admin.bit.jeap.security.test.client.configuration;

import ch.admin.bit.jeap.security.restclient.OAuth2RestClientConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportAutoConfiguration(exclude = {OAuth2RestClientConfiguration.class})
public class DisableJeapOAuth2ClientConfiguration {
}
