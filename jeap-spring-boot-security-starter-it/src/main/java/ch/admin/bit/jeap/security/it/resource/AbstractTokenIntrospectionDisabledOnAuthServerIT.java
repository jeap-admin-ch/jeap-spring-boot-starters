package ch.admin.bit.jeap.security.it.resource;

import com.nimbusds.jwt.JWT;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

@TestPropertySource(properties = {"jeap.security.oauth2.resourceserver.authorization-server.introspection.mode=none"})
public class AbstractTokenIntrospectionDisabledOnAuthServerIT extends TokenIntrospectionITBase {

    protected AbstractTokenIntrospectionDisabledOnAuthServerIT(int serverPort, String context) {
        super(serverPort, context);
    }

    protected void testGetAuth_whenReadRoleInTokenAnIntrospectionDisabledOnAuthServer_ThenAccessGrantedWithoutIntrospection() {
        final List<String> userroles = List.of(SEMANTIC_AUTH_READ_ROLE); // grants access to /auth
        final JWT jwt = createBearerToken(Map.of(USER_ROLES_CLAIM_NAME, userroles));
        // Stub token introspection with a response that would prohibit access to the resource (active=false)
        stubTokenIntrospectionRequest(jwt, false, null); // token not active

        // Assert that access was granted and no token introspection request was made
        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.OK); // would be UNAUTHORIZED if introspection was done
        verifyTokenIntrospectionRequest(0); // would be 1 if introspection was done
    }

}
