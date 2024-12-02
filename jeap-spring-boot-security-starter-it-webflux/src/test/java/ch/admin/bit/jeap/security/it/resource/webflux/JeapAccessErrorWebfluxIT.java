package ch.admin.bit.jeap.security.it.resource.webflux;

import ch.admin.bit.jeap.security.it.resource.AbstractJeapAccessErrorIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=9013"})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
class JeapAccessErrorWebfluxIT extends AbstractJeapAccessErrorIT {

	JeapAccessErrorWebfluxIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
		super(serverPort, context);
	}

	@Test
	protected void testGetAuth_whenAuthorizedToAccessErrorEndpoint_ThenHttp500() {
		super.testGetAuth_whenAuthorizedToAccessErrorEndpoint_ThenHttp500();
	}

	@Test
	protected void testGetAuth_whenNoBearerTokenSet_ThenUnauthorized() {
		super.testGetAuth_whenNoBearerTokenSet_ThenUnauthorized();
	}

	@Test
	protected void testGetAuth_whenBearerTokenIsMissingRoleToAccessErrorEndpoint_ThenForbidden() {
		super.testGetAuth_whenBearerTokenIsMissingRoleToAccessErrorEndpoint_ThenForbidden();
	}

}
