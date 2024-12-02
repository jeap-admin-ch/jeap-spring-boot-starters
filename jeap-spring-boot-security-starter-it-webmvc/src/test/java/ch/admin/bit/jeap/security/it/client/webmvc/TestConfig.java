package ch.admin.bit.jeap.security.it.client.webmvc;

import ch.admin.bit.jeap.security.it.resource.OAuth2TestGateway;
import ch.admin.bit.jeap.security.it.resource.BearerTokenResource;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import({BearerTokenResource.class, OAuth2TestGateway.class})
@SpringBootApplication
public class TestConfig {
}
