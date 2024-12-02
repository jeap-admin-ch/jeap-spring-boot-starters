package ch.admin.bit.jeap.security.it.resource.webmvc;

import ch.admin.bit.jeap.security.it.resource.AccessTokensFromTwoDifferentAuthServersITBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
        "server.port=8011",
        "jeap.security.oauth2.resourceserver.auth-servers[0].issuer=http://localhost:8098/auth",
        "jeap.security.oauth2.resourceserver.auth-servers[1].issuer=http://localhost:8099/auth"
})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
class AccessTokensFromTwoDifferentAuthServersWebmvcIT extends AccessTokensFromTwoDifferentAuthServersITBase {

    protected AccessTokensFromTwoDifferentAuthServersWebmvcIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
        super(serverPort, context);
    }

    @Test
    protected void testGetAuth_whenBothMockServerIssuersConfigured_thenAccessGrantedForTokensOfBothMockServers() {
        super.assertHttpStatusWithTokenOnGet(createBearerTokenForMockServer1(), HttpStatus.OK);
        super.assertHttpStatusWithTokenOnGet(createBearerTokenForMockServer2(), HttpStatus.OK);
    }

}