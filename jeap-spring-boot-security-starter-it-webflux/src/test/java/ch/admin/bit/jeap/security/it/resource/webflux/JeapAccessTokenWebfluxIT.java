package ch.admin.bit.jeap.security.it.resource.webflux;

import ch.admin.bit.jeap.security.it.resource.AbstractJeapAccessTokenIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=9001"})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
class JeapAccessTokenWebfluxIT extends AbstractJeapAccessTokenIT {

	JeapAccessTokenWebfluxIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
		super(serverPort, context);
	}

	@Test
	protected void testGetAuth_whenAuthServerJeapTokenWithUserInfoAndRoleAuthRead_thenAccessGrantedAndUserInfoAndRolesCorrect() {
		super.testGetAuth_whenAuthServerJeapTokenWithUserInfoAndRoleAuthRead_thenAccessGrantedAndUserInfoAndRolesCorrect();
	}

	@Test
	protected void testGetAuth_whenAuthServerJeapTokenWithBpRoleAuthRead_thenAccessGrantedAndRolesCorrect() {
		super.testGetAuth_whenAuthServerJeapTokenWithBpRoleAuthRead_thenAccessGrantedAndRolesCorrect();
	}

}
