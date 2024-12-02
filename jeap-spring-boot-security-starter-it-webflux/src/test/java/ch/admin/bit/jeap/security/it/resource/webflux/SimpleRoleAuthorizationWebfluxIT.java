package ch.admin.bit.jeap.security.it.resource.webflux;

import ch.admin.bit.jeap.security.it.resource.AbstractSimpleRoleAuthorizationIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=9007"})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
public class SimpleRoleAuthorizationWebfluxIT extends AbstractSimpleRoleAuthorizationIT {

    protected SimpleRoleAuthorizationWebfluxIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
        super(serverPort, context);
    }

    @Test
    protected void testGetAuth_whenWithUserRoleAuthRead_thenAccessGranted() {
        super.testGetAuth_whenWithUserRoleAuthRead_thenAccessGranted();
    }

    @Test
    protected void testGetAuth_whenOnlyWithUserRoleDifferentThanAuthRead_thenAccessDenied() {
        super.testGetAuth_whenOnlyWithUserRoleDifferentThanAuthRead_thenAccessDenied();
    }

    @Test
    protected void testGetAuthForPartner_whenWithBpRoleAuthReadForQueriedPartner_thenAccessGranted() {
        super.testGetAuthForPartner_whenWithBpRoleAuthReadForQueriedPartner_thenAccessGranted();
    }

    @Test
    protected void testGetAuthForPartner_whenWithoutBpRoleAuthReadForQueriedPartner_thenAccessDenied() {
        super.testGetAuthForPartner_whenWithoutBpRoleAuthReadForQueriedPartner_thenAccessDenied();
    }

}
