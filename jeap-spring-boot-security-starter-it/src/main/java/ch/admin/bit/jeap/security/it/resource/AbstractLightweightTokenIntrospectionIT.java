package ch.admin.bit.jeap.security.it.resource;

import com.nimbusds.jwt.JWT;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;


@TestPropertySource(properties = {"jeap.security.oauth2.resourceserver.introspection-mode=lightweight"})
public class AbstractLightweightTokenIntrospectionIT extends TokenIntrospectionITBase {

    protected static final String ROLES_PRUNED_CHARS_CLAIM_NAME = "roles_pruned_chars";
    protected static final String SCOPE_CLAIM_NAME = "scope";
    protected static final String LIGHTWEIGHT_SCOPE_NAME = "lightweight";

    protected AbstractLightweightTokenIntrospectionIT(int serverPort, String context) {
        super(serverPort, context);
    }

    protected void testGetAuth_whenRolesPrunedCharsInTokenAndReadRoleInActiveIntrospectionResponse_ThenReadRoleInAuthenticationAndAccessGranted() {
        final JWT jwt = createBearerToken(Map.of(ROLES_PRUNED_CHARS_CLAIM_NAME, 1000)); // pruned roles claim present
        final List<String> userroles = List.of(SEMANTIC_AUTH_READ_ROLE); // grants access to /auth
        final Map<String, Object> introspectedRolesClaims = Map.of(USER_ROLES_CLAIM_NAME, userroles);
        stubTokenIntrospectionRequest(jwt, true, introspectedRolesClaims); // token active

        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.OK)
            .body(USER_ROLES_CLAIM_NAME, equalTo(userroles));
        verifyTokenIntrospectionRequest(1);
    }

    protected void testGetAuth_whenRolesPrunedCharsNotInTokenAndReadRoleInActiveIntrospectionResponse_ThenNoIntrospectionAndReadRoleNotInAuthenticationAndAccessDenied() {
        final JWT jwt = createBearerToken(null); // pruned roles claim not present
        final List<String> userroles = List.of(SEMANTIC_AUTH_READ_ROLE); // grants access to /auth
        final Map<String, Object> introspectedRolesClaims = Map.of(USER_ROLES_CLAIM_NAME, userroles);
        stubTokenIntrospectionRequest(jwt, true, introspectedRolesClaims); // token active

        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.FORBIDDEN);
        verifyTokenIntrospectionRequest(0);
    }

    protected void testGetAuth_whenLightweightScopeInTokenAndReadRoleInActiveIntrospectionResponse_ThenReadRoleInAuthenticationAndAccessGranted() {
        final JWT jwt = createBearerToken(Map.of(SCOPE_CLAIM_NAME, "openid " + LIGHTWEIGHT_SCOPE_NAME)); // lightweight scope present
        final List<String> userroles = List.of(SEMANTIC_AUTH_READ_ROLE); // grants access to /auth
        final Map<String, Object> introspectedRolesClaims = Map.of(USER_ROLES_CLAIM_NAME, userroles);
        stubTokenIntrospectionRequest(jwt, true, introspectedRolesClaims); // token active

        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.OK)
                .body(USER_ROLES_CLAIM_NAME, equalTo(userroles));
        verifyTokenIntrospectionRequest(1);
    }

    protected void testGetAuth_wheLightweightScopeNotInTokenAndReadRoleInActiveIntrospectionResponse_ThenNoIntrospectionAndReadRoleNotInAuthenticationAndAccessDenied() {
        final JWT jwt = createBearerToken(Map.of(SCOPE_CLAIM_NAME, "openid")); // lightweight scope not present
        final List<String> userroles = List.of(SEMANTIC_AUTH_READ_ROLE); // grants access to /auth
        final Map<String, Object> introspectedRolesClaims = Map.of(USER_ROLES_CLAIM_NAME, userroles);
        stubTokenIntrospectionRequest(jwt, true, introspectedRolesClaims); // token active

        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.FORBIDDEN);
        verifyTokenIntrospectionRequest(0);
    }

}
