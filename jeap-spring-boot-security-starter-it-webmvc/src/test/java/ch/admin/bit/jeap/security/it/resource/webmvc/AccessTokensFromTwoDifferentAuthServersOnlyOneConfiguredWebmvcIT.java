package ch.admin.bit.jeap.security.it.resource.webmvc;

import ch.admin.bit.jeap.security.it.resource.AccessTokensFromTwoDifferentAuthServersITBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
        "server.port=8012",
        "jeap.security.oauth2.resourceserver.auth-servers[0].issuer=http://localhost:8098/auth"
})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
class AccessTokensFromTwoDifferentAuthServersOnlyOneConfiguredWebmvcIT extends AccessTokensFromTwoDifferentAuthServersITBase {

    protected AccessTokensFromTwoDifferentAuthServersOnlyOneConfiguredWebmvcIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
        super(serverPort, context);
    }

    @Test
    protected void testGetAuth_whenOnlyOneMockServerIssuerConfigured_thenAccessOnlyGrantedForTokensOfTheConfiguredMockServer() {
        super.assertHttpStatusWithTokenOnGet(createBearerTokenForMockServer1(), HttpStatus.OK);
        super.assertHttpStatusWithTokenOnGet(createBearerTokenForMockServer2(), HttpStatus.UNAUTHORIZED);
    }

}