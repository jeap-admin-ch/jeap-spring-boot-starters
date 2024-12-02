package ch.admin.bit.jeap.security.test.client.configuration;

import ch.admin.bit.jeap.security.client.OAuth2ClientConfiguration;
import ch.admin.bit.jeap.security.restclient.OAuth2RestClientConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportAutoConfiguration(exclude = {OAuth2ClientConfiguration.class, OAuth2RestClientConfiguration.class})
public class DisableJeapOAuth2ClientConfiguration {
}
