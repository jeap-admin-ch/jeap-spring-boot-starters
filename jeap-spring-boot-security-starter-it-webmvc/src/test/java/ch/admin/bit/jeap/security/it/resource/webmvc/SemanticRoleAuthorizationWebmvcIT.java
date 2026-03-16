package ch.admin.bit.jeap.security.it.resource.webmvc;

import ch.admin.bit.jeap.security.it.resource.AbstractSemanticRoleAuthorizationIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=8008"})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
public class SemanticRoleAuthorizationWebmvcIT extends AbstractSemanticRoleAuthorizationIT {

    protected SemanticRoleAuthorizationWebmvcIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
        super(serverPort, context);
    }

    @Test
    @Override
    protected void testGetAuth_whenWithUserRoleAuthRead_thenAccessGranted() {
        super.testGetAuth_whenWithUserRoleAuthRead_thenAccessGranted();
    }

    @Test
    @Override
    protected void testGetAuth_whenOnlyWithUserRoleDifferentThanAuthRead_thenAccessDenied() {
        super.testGetAuth_whenOnlyWithUserRoleDifferentThanAuthRead_thenAccessDenied();
    }

    @Test
    @Override
    protected void testGetAuthForPartner_whenWithBpRoleAuthReadForQueriedPartner_thenAccessGranted() {
        super.testGetAuthForPartner_whenWithBpRoleAuthReadForQueriedPartner_thenAccessGranted();
    }

    @Test
    @Override
    protected void testGetAuthForPartner_whenWithoutBpRoleAuthReadForQueriedPartner_thenAccessDenied() {
        super.testGetAuthForPartner_whenWithoutBpRoleAuthReadForQueriedPartner_thenAccessDenied();
    }

    @Test
    @Override
    protected void testGetAuth_whenWithUserRoleAuthReadAlternativeSyntax_thenAccessGranted() {
        super.testGetAuth_whenWithUserRoleAuthReadAlternativeSyntax_thenAccessGranted();
    }

    @Test
    @Override
    protected void testGetAuth_whenOnlyWithUserRoleDifferentThanAuthReadAlternativeSyntax_thenAccessDenied() {
        super.testGetAuth_whenOnlyWithUserRoleDifferentThanAuthReadAlternativeSyntax_thenAccessDenied();
    }

    @Test
    @Override
    protected void testGetAuthForPartner_whenWithBpRoleAuthReadForQueriedPartnerAlternativeSyntax_thenAccessGranted() {
        super.testGetAuthForPartner_whenWithBpRoleAuthReadForQueriedPartnerAlternativeSyntax_thenAccessGranted();
    }

    @Test
    @Override
    protected void testGetAuthForPartner_whenWithoutBpRoleAuthReadForQueriedPartnerAlternativeSyntax_thenAccessDenied() {
        super.testGetAuthForPartner_whenWithoutBpRoleAuthReadForQueriedPartnerAlternativeSyntax_thenAccessDenied();
    }

}
