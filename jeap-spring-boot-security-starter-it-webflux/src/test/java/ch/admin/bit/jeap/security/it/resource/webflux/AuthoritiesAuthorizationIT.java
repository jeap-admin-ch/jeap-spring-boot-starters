package ch.admin.bit.jeap.security.it.resource.webflux;

import ch.admin.bit.jeap.security.it.resource.AbstractAuthoritiesAuthorizationIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=9006"})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
public class AuthoritiesAuthorizationIT extends AbstractAuthoritiesAuthorizationIT {

    AuthoritiesAuthorizationIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
        super(serverPort, context);
    }

    @Test
    void testAccessGranted() {
        super.testGetAuth_whenApplicationResolvesAdminAuthorities_thenAccessGranted();
    }

    @Test
    void testAccessRejected() {
        super.testGetAuth_whenApplicationResolvesNoAuthorities_thenAccessRejected();
    }

}
