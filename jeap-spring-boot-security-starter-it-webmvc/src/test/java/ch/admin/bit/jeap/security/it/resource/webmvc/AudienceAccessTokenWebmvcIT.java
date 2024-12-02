package ch.admin.bit.jeap.security.it.resource.webmvc;

import ch.admin.bit.jeap.security.it.resource.AbstractAudienceAccessTokenIT;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=8003"})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
class AudienceAccessTokenWebmvcIT extends AbstractAudienceAccessTokenIT {

	AudienceAccessTokenWebmvcIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
		super(serverPort, context);
	}

	@ParameterizedTest
	@EnumSource(JeapAuthenticationContext.class)
	protected void testGetAuth_whenJeapTokenAudienceEmptyInContext_thenAccessGranted(JeapAuthenticationContext context) {
		super.testGetAuth_whenJeapTokenAudienceEmptyInContext_thenAccessGranted(context);
	}

	@ParameterizedTest
	@EnumSource(JeapAuthenticationContext.class)
	protected void testGetAuth_whenJeapTokenAudienceContainsAppNameInContext_thenAccessGranted(JeapAuthenticationContext context) {
		super.testGetAuth_whenJeapTokenAudienceContainsAppNameInContext_thenAccessGranted(context);
	}

	@ParameterizedTest
	@EnumSource(value = JeapAuthenticationContext.class, names = {"USER", "SYS"})
	protected void testGetAuth_whenJeapTokenAudienceDoesNotContainsAppNameInContext_thenAccessDenied(JeapAuthenticationContext context) {
		super.testGetAuth_whenJeapTokenAudienceDoesNotContainsAppNameInContext_thenAccessDenied(context);
	}

	@ParameterizedTest
	@EnumSource(value = JeapAuthenticationContext.class, names = {"B2B"})
	protected void testGetAuth_whenJeapTokenAudienceDoesNotContainsAppNameInContext_thenAccessGranted(JeapAuthenticationContext context) {
		// B2B tokens are not audience checked as there is currently no way to specify the token audience in the B2B gateway configuration
		super.testGetAuth_whenJeapTokenAudienceDoesNotContainsAppNameInContext_thenAccessGranted(context);
	}

}
