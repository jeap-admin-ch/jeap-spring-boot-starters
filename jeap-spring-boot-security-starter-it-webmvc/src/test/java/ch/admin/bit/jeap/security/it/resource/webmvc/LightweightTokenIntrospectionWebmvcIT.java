package ch.admin.bit.jeap.security.it.resource.webmvc;

import ch.admin.bit.jeap.security.it.resource.AbstractLightweightTokenIntrospectionIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = { "server.port=8031" })
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
class LightweightTokenIntrospectionWebmvcIT extends AbstractLightweightTokenIntrospectionIT {

    protected LightweightTokenIntrospectionWebmvcIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
        super(serverPort, context);
    }

    @Test
    protected void testGetAuth_whenRolesPrunedCharsInTokenAndReadRoleInActiveIntrospectionResponse_ThenReadRoleInAuthenticationAndAccessGranted() {
       super.testGetAuth_whenRolesPrunedCharsInTokenAndReadRoleInActiveIntrospectionResponse_ThenReadRoleInAuthenticationAndAccessGranted();
    }

    @Test
    protected void testGetAuth_whenRolesPrunedCharsNotInTokenAndReadRoleInActiveIntrospectionResponse_ThenNoIntrospectionAndReadRoleNotInAuthenticationAndAccessDenied() {
        super.testGetAuth_whenRolesPrunedCharsNotInTokenAndReadRoleInActiveIntrospectionResponse_ThenNoIntrospectionAndReadRoleNotInAuthenticationAndAccessDenied();
    }

    @Test
    protected void testGetAuth_whenLightweightScopeInTokenAndReadRoleInActiveIntrospectionResponse_ThenReadRoleInAuthenticationAndAccessGranted() {
        super.testGetAuth_whenLightweightScopeInTokenAndReadRoleInActiveIntrospectionResponse_ThenReadRoleInAuthenticationAndAccessGranted();
    }

    @Test
    protected void testGetAuth_wheLightweightScopeNotInTokenAndReadRoleInActiveIntrospectionResponse_ThenNoIntrospectionAndReadRoleNotInAuthenticationAndAccessDenied() {
        super.testGetAuth_wheLightweightScopeNotInTokenAndReadRoleInActiveIntrospectionResponse_ThenNoIntrospectionAndReadRoleNotInAuthenticationAndAccessDenied();
    }

}
