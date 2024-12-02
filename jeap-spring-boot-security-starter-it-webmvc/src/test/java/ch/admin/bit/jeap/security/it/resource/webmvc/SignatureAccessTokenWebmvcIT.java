package ch.admin.bit.jeap.security.it.resource.webmvc;

import ch.admin.bit.jeap.security.it.resource.AbstractSignatureAccessTokenIT;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=8004"})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
public class SignatureAccessTokenWebmvcIT extends AbstractSignatureAccessTokenIT {

	protected SignatureAccessTokenWebmvcIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
		super(serverPort, context);
	}

	@ParameterizedTest
	@EnumSource(JeapAuthenticationContext.class)
	protected void testGetAuth_whenJeapTokenSignedWithCorrectKeyInContext_thenAccessGranted(JeapAuthenticationContext context) {
		super.testGetAuth_whenJeapTokenSignedWithCorrectKeyInContext_thenAccessGranted(context);
	}

	@ParameterizedTest
	@EnumSource(JeapAuthenticationContext.class)
	protected void testGetAuth_whenJeapTokenSignedWithWrongKeyInContext_thenAccessDenied(JeapAuthenticationContext context) {
		super.testGetAuth_whenJeapTokenSignedWithWrongKeyInContext_thenAccessDenied(context);
	}

}
