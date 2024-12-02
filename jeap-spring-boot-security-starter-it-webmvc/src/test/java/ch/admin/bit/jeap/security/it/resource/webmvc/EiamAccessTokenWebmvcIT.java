package ch.admin.bit.jeap.security.it.resource.webmvc;

import ch.admin.bit.jeap.security.it.resource.AbstractEiamAccessTokenIT;
import ch.admin.bit.jeap.security.it.resource.EiamClaimSetConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("eiam")
@Import(EiamClaimSetConverter.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
      		    properties = {  "server.port=8002",
								"jeap.security.oauth2.resourceserver.authorization-server.claim-set-converter-name=eiamClaimSetConverter"})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
class EiamAccessTokenWebmvcIT extends AbstractEiamAccessTokenIT {

	EiamAccessTokenWebmvcIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
		super(serverPort, context);
	}

	@Test
    protected void testGetAuth_whenAuthServerEiamTokenWithRoleAuthRead_thenConvertedCorrectlyToJeapTokenAndAccessGranted() {
		super.testGetAuth_whenAuthServerEiamTokenWithRoleAuthRead_thenConvertedCorrectlyToJeapTokenAndAccessGranted();
	}

	@Test
	protected void testGetAuth_whenAuthServerJeapTokenWithRoleAuthRead_thenCtxSetByEiamClaimSetConverterAndAccessGranted() {
		super.testGetAuth_whenAuthServerJeapTokenWithRoleAuthRead_thenCtxSetByEiamClaimSetConverterAndAccessGranted();
	}

	@Test
	protected void testGetAuth_whenAuthServerEiamTokenWithoutRoleAuthRead_thenAccessDenied() {
		super.testGetAuth_whenAuthServerEiamTokenWithoutRoleAuthRead_thenAccessDenied();
	}

	@Test
	protected void testGetAuth_whenB2BJeapTokenWithRoleAuthRead_thenEiamClaimSetConverterNotActiveAndAccessGranted() {
		super.testGetAuth_whenB2BJeapTokenWithRoleAuthRead_thenEiamClaimSetConverterNotActiveAndAccessGranted();
	}

	@Test
	protected void testGetAuth_whenB2BEiamTokenWithRoleAuthRead_thenAccessDeniedAsNoJeapTokenClaimsArePresent() {
		super.testGetAuth_whenB2BEiamTokenWithRoleAuthRead_thenAccessDeniedAsNoJeapTokenClaimsArePresent();
	}

}
