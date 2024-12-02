package ch.admin.bit.jeap.security.test.resource.configuration;

import ch.admin.bit.jeap.security.resource.configuration.MvcSecurityConfiguration;
import ch.admin.bit.jeap.security.resource.configuration.WebFluxSecurityConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportAutoConfiguration(exclude = {MvcSecurityConfiguration.class, WebFluxSecurityConfiguration.class
})
public class DisableJeapOAuth2SecurityConfiguration {
}
