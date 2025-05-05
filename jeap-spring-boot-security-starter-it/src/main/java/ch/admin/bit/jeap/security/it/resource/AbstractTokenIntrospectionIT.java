package ch.admin.bit.jeap.security.it.resource;

import com.nimbusds.jwt.JWT;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

public class AbstractTokenIntrospectionIT extends TokenIntrospectionITBase {

    protected AbstractTokenIntrospectionIT(int serverPort, String context) {
        super(serverPort, context);
    }

    protected void testGetAuth_whenNoRolesInTokenAndReadRoleInActiveIntrospectionResponse_ThenReadRoleInAuthenticationAndAccessGranted() {
        final JWT jwt = createBearerToken(null);
        final List<String> userroles = List.of(SEMANTIC_AUTH_READ_ROLE); // grants access to /auth
        final Map<String, List<String>> bproles = Map.of("bp1", List.of(SEMANTIC_OTHER_ROLE)); // additional role
        final var introspectedRolesClaims = Map.of(USER_ROLES_CLAIM_NAME, userroles,BP_ROLES_CLAIM_NAME, bproles);
        stubTokenIntrospectionRequest(jwt, true, introspectedRolesClaims); // token active

        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.OK)
            .body(USER_ROLES_CLAIM_NAME, equalTo(userroles))
            .body(BP_ROLES_CLAIM_NAME, equalTo(bproles));
        verifyTokenIntrospectionRequest(1);
    }

    protected void testGetAuth_whenReadRoleInTokenAndActiveIntrospectionResponse_ThenReadRoleInAuthenticationAndAccessGranted() {
        final List<String> userroles = List.of(SEMANTIC_AUTH_READ_ROLE); // grants access to /auth
        final JWT jwt = createBearerToken(Map.of(USER_ROLES_CLAIM_NAME, userroles));
        final Map<String, List<String>> bproles = Map.of("bp1", List.of(SEMANTIC_OTHER_ROLE)); // additional role
        final Map<String, Object> introspectedRolesClaims = Map.of(BP_ROLES_CLAIM_NAME, bproles);
        stubTokenIntrospectionRequest(jwt, true, introspectedRolesClaims); // token active

        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.OK)
                .body(USER_ROLES_CLAIM_NAME, equalTo(userroles))
                .body(BP_ROLES_CLAIM_NAME, equalTo(bproles));
        verifyTokenIntrospectionRequest(1);
    }

    protected void testGetAuth_whenNoRolesInTokenAndReadRoleInNonActiveIntrospectionResponse_ThenUnauthorized() {
        final JWT jwt = createBearerToken(null);
        final List<String> userroles = List.of(SEMANTIC_AUTH_READ_ROLE); // grants access to /auth
        stubTokenIntrospectionRequest(jwt, false, Map.of(USER_ROLES_CLAIM_NAME, userroles)); // token not active

        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.UNAUTHORIZED);
        verifyTokenIntrospectionRequest(1);
    }

    protected void testGetAuth_whenReadRoleInTokenAndNonActiveIntrospectionResponse_ThenUnauthorized() {
        final List<String> userroles = List.of(SEMANTIC_AUTH_READ_ROLE); // grants access to /auth
        final JWT jwt = createBearerToken(Map.of(USER_ROLES_CLAIM_NAME, userroles));
        stubTokenIntrospectionRequest(jwt, false, null); // token not active

        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.UNAUTHORIZED);
        verifyTokenIntrospectionRequest(1);
    }

    protected void testGetAuth_whenNoRolesInTokenAndNoRolesInActiveIntrospectionResponse_ThenAccessDenied() {
        final JWT jwt = createBearerToken(null);
        stubTokenIntrospectionRequest(jwt, true, null);

        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.FORBIDDEN);
        verifyTokenIntrospectionRequest(1);
    }

    protected void testGetAuth_whenNoRolesInTokenAndNoRolesInInactiveIntrospectionResponse_ThenUnauthorized() {
        final JWT jwt = createBearerToken(null);
        stubTokenIntrospectionRequest(jwt, false, null);

        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.UNAUTHORIZED);
        verifyTokenIntrospectionRequest(1);
    }

    protected void testGetAuth_whenIntrospectionRequestError_ThenInternalServerError() {
        final JWT jwt = createBearerToken(null);
        stubTokenIntrospectionErrorResponseRequest();

        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.INTERNAL_SERVER_ERROR);
        verifyTokenIntrospectionRequest(1);
    }

    protected void testGetAuth_whenIntrospectionRequestTimesOut_ThenInternalServerError() {
        final JWT jwt = createBearerToken(null);
        stubTokenIntrospectionRequestWithDelayedResponse(jwt, 2000); // longer than the configured introspection timeouts

        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.INTERNAL_SERVER_ERROR);
        verifyTokenIntrospectionRequest(1);
    }

}


