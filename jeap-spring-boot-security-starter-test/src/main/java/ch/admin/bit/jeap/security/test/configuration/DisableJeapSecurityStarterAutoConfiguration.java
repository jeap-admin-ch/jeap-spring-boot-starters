package ch.admin.bit.jeap.security.test.configuration;

import ch.admin.bit.jeap.security.test.client.configuration.DisableJeapOAuth2ClientConfiguration;
import ch.admin.bit.jeap.security.test.resource.configuration.DisableJeapDenyAllSecurityConfiguration;
import ch.admin.bit.jeap.security.test.resource.configuration.DisableJeapOAuth2SecurityConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({DisableJeapOAuth2SecurityConfiguration.class, DisableJeapOAuth2ClientConfiguration.class, DisableJeapDenyAllSecurityConfiguration.class})
public class DisableJeapSecurityStarterAutoConfiguration {
}
