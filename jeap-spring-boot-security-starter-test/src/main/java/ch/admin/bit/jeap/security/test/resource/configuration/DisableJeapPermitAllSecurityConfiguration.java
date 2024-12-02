package ch.admin.bit.jeap.security.test.resource.configuration;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportAutoConfiguration(exclude = PermitAllWebSecurityConfiguration.class)
public class DisableJeapPermitAllSecurityConfiguration {
}
