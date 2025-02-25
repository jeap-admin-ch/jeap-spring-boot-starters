package ch.admin.bit.jeap.security.user;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan
@ConditionalOnProperty(value = "jeap.security.oauth2.current-user-endpoint.enabled")
public class JeapCurrentUserEndpointConfiguration {

}
