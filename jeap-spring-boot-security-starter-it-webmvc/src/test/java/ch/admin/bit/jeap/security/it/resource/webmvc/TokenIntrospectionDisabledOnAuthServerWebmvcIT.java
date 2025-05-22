package ch.admin.bit.jeap.security.it.resource.webmvc;

import ch.admin.bit.jeap.security.it.resource.AbstractTokenIntrospectionDisabledOnAuthServerIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = { "server.port=8032"})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
class TokenIntrospectionDisabledOnAuthServerWebmvcIT extends AbstractTokenIntrospectionDisabledOnAuthServerIT {

    protected TokenIntrospectionDisabledOnAuthServerWebmvcIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
        super(serverPort, context);
    }

    @Test
    protected void testGetAuth_whenReadRoleInTokenAnIntrospectionDisabledOnAuthServer_ThenAccessGrantedWithoutIntrospection() {
       super.testGetAuth_whenReadRoleInTokenAnIntrospectionDisabledOnAuthServer_ThenAccessGrantedWithoutIntrospection();
    }

}
