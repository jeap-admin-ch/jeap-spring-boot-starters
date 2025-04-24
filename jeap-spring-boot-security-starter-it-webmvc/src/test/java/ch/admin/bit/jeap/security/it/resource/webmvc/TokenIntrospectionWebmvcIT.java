package ch.admin.bit.jeap.security.it.resource.webmvc;

import ch.admin.bit.jeap.security.it.resource.AbstractTokenIntrospectionIT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = { "server.port=8030"})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
class TokenIntrospectionWebmvcIT extends AbstractTokenIntrospectionIT {

    protected TokenIntrospectionWebmvcIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
        super(serverPort, context);
    }

    @Test
    protected void testGetAuth_whenNoRolesInTokenAndReadRoleInActiveIntrospectionResponse_ThenReadRoleInAuthenticationAndAccessGranted() {
       super.testGetAuth_whenNoRolesInTokenAndReadRoleInActiveIntrospectionResponse_ThenReadRoleInAuthenticationAndAccessGranted();
    }

    @Test
    protected void testGetAuth_whenReadRoleInTokenAndActiveIntrospectionResponse_ThenReadRoleInAuthenticationAndAccessGranted() {
        super.testGetAuth_whenReadRoleInTokenAndActiveIntrospectionResponse_ThenReadRoleInAuthenticationAndAccessGranted();
    }

    @Test
    protected void testGetAuth_whenNoRolesInTokenAndReadRoleInNonActiveIntrospectionResponse_ThenUnauthorized() {
        super.testGetAuth_whenNoRolesInTokenAndReadRoleInNonActiveIntrospectionResponse_ThenUnauthorized();
    }

    @Test
    protected void testGetAuth_whenReadRoleInTokenAndNonActiveIntrospectionResponse_ThenUnauthorized() {
        super.testGetAuth_whenReadRoleInTokenAndNonActiveIntrospectionResponse_ThenUnauthorized();
    }

    @Test
    protected void testGetAuth_whenNoRolesInTokenAndNoRolesInActiveIntrospectionResponse_ThenAccessDenied() {
        super.testGetAuth_whenNoRolesInTokenAndNoRolesInActiveIntrospectionResponse_ThenAccessDenied();
    }

    @Test
    protected void testGetAuth_whenNoRolesInTokenAndNoRolesInInactiveIntrospectionResponse_ThenUnauthorized() {
        super.testGetAuth_whenNoRolesInTokenAndNoRolesInInactiveIntrospectionResponse_ThenUnauthorized();
    }

    @Test
    protected void testGetAuth_whenIntrospectionRequestError_ThenInternalServerError() {
        super.testGetAuth_whenIntrospectionRequestError_ThenInternalServerError();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    protected void testGetAuth_whenIntrospectionRequestTimesOut_ThenInternalServerError() {
        super.testGetAuth_whenIntrospectionRequestTimesOut_ThenInternalServerError();
    }

}
