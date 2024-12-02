package ch.admin.bit.jeap.security.it.resource.webmvc;

import ch.admin.bit.jeap.security.it.resource.AbstractTimestampsAccessTokenIT;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=8005"})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
public class TimestampsAccessTokenWebmvcIT extends AbstractTimestampsAccessTokenIT {

	protected TimestampsAccessTokenWebmvcIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
		super(serverPort, context);
	}

	@ParameterizedTest
	@EnumSource(JeapAuthenticationContext.class)
	protected void testGetAuth_whenJeapTokenValidInContext_thenAccessGranted(JeapAuthenticationContext context) {
		super.testGetAuth_whenJeapTokenValidInContext_thenAccessGranted(context);
	}

	@ParameterizedTest
	@EnumSource(JeapAuthenticationContext.class)
	protected void testGetAuth_whenJeapTokenExpiredInContext_thenAccessDenied(JeapAuthenticationContext context) {
		super.testGetAuth_whenJeapTokenExpiredInContext_thenAccessDenied(context);
	}

	@ParameterizedTest
	@EnumSource(JeapAuthenticationContext.class)
	protected void testGetAuth_whenJeapTokenExpiredWithinLeewayInContext_thenAccessGranted(JeapAuthenticationContext context) {
		super.testGetAuth_whenJeapTokenExpiredWithinLeewayInContext_thenAccessGranted(context);
	}

	@ParameterizedTest
	@EnumSource(JeapAuthenticationContext.class)
	protected void testGetAuth_whenJeapTokenNotBeforeIsAfterNowInContext_thenAccessDenied(JeapAuthenticationContext context) {
		super. testGetAuth_whenJeapTokenNotBeforeIsAfterNowInContext_thenAccessDenied(context);
	}

	@ParameterizedTest
	@EnumSource(JeapAuthenticationContext.class)
	protected void testGetAuth_whenJeapTokenNotBeforeIsAfterNowButWithinLeewayInContext_thenAccessDenied(JeapAuthenticationContext context) {
		super.testGetAuth_whenJeapTokenNotBeforeIsAfterNowButWithinLeewayInContext_thenAccessDenied(context);
	}

}
