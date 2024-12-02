package ch.admin.bit.jeap.security.test.configuration;

import ch.admin.bit.jeap.security.test.client.configuration.JeapOAuth2IntegrationTestClientConfiguration;
import ch.admin.bit.jeap.security.test.resource.configuration.JeapOAuth2IntegrationTestResourceConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({JeapOAuth2IntegrationTestResourceConfiguration.class, JeapOAuth2IntegrationTestClientConfiguration.class})
public class JeapOAuth2IntegrationTestConfiguration {
}
