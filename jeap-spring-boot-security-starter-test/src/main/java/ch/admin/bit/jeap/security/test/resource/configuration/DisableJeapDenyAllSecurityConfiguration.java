package ch.admin.bit.jeap.security.test.resource.configuration;

import ch.admin.bit.jeap.security.resource.configuration.DefaultDenyAllWebSecurityConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportAutoConfiguration(exclude = DefaultDenyAllWebSecurityConfiguration.class)
public class DisableJeapDenyAllSecurityConfiguration {
}
