package ch.admin.bit.jeap.security.test.resource.configuration;

import ch.admin.bit.jeap.security.resource.configuration.MvcSecurityConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportAutoConfiguration(exclude = MvcSecurityConfiguration.class)
public class DisableJeapOAuth2SecurityConfiguration {
}
