package ch.admin.bit.jeap.security.it.client.webflux;

import ch.admin.bit.jeap.security.it.resource.OAuth2TestGateway;
import ch.admin.bit.jeap.security.it.resource.BearerTokenResource;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import({OAuth2TestGateway.class, BearerTokenResource.class})
@SpringBootApplication
public class TestConfig {
}
