package ch.admin.bit.jeap.security.it.resource;

import com.nimbusds.jwt.JWT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;

/**
 * Token introspection caching tests. Expect an application context with an introspection mode that introspects
 * every token (e.g. 'always') and the token introspection cache enabled for the authorization server mocked by the
 * OAuth2 mock server.
 */
public abstract class AbstractTokenIntrospectionCachingIT extends TokenIntrospectionITBase {

    private static final String TOKEN_NONCE_CLAIM_NAME = "test-token-nonce";

    protected AbstractTokenIntrospectionCachingIT(int serverPort, String context) {
        super(serverPort, context);
    }

    @Test
    protected void testGetAuth_whenSameTokenPresentedTwice_thenIntrospectedOnlyOnce() {
        final JWT jwt = createUniqueBearerToken();
        final List<String> userroles = List.of(SEMANTIC_AUTH_READ_ROLE); // grants access to /auth
        stubTokenIntrospectionRequest(jwt, true, Map.of(USER_ROLES_CLAIM_NAME, userroles));

        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.OK)
                .body(USER_ROLES_CLAIM_NAME, equalTo(userroles));
        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.OK)
                .body(USER_ROLES_CLAIM_NAME, equalTo(userroles));
        verifyTokenIntrospectionRequest(1);
    }

    @Test
    protected void testGetAuth_whenDifferentTokensPresented_thenEachIntrospected() {
        final JWT jwt = createUniqueBearerToken();
        final JWT otherJwt = createUniqueBearerToken();
        final List<String> userroles = List.of(SEMANTIC_AUTH_READ_ROLE); // grants access to /auth
        stubTokenIntrospectionRequest(jwt, true, Map.of(USER_ROLES_CLAIM_NAME, userroles));
        stubTokenIntrospectionRequest(otherJwt, true, Map.of(USER_ROLES_CLAIM_NAME, userroles));

        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.OK);
        assertHttpStatusAndUserInfoWithTokenOnGet(otherJwt.serialize(), HttpStatus.OK);
        verifyTokenIntrospectionRequest(2);
    }

    @Test
    protected void testGetAuth_whenInactiveTokenPresentedTwice_thenIntrospectedTwiceAndUnauthorized() {
        final JWT jwt = createUniqueBearerToken();
        stubTokenIntrospectionRequest(jwt, false, Map.of(USER_ROLES_CLAIM_NAME, List.of(SEMANTIC_AUTH_READ_ROLE)));

        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.UNAUTHORIZED);
        assertHttpStatusAndUserInfoWithTokenOnGet(jwt.serialize(), HttpStatus.UNAUTHORIZED);
        verifyTokenIntrospectionRequest(2);
    }

    /**
     * Create a token with a unique nonce claim. The token introspection cache outlives a single test, so every test
     * needs tokens that differ from the tokens of the other tests.
     */
    private JWT createUniqueBearerToken() {
        return createBearerToken(Map.of(TOKEN_NONCE_CLAIM_NAME, UUID.randomUUID().toString()));
    }

}
