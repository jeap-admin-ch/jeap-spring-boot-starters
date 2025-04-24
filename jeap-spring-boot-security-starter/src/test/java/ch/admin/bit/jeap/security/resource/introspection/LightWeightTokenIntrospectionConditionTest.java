package ch.admin.bit.jeap.security.resource.introspection;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;

import static ch.admin.bit.jeap.security.resource.introspection.LightweightTokenIntrospectionCondition.ROLES_PRUNED_CHARS_CLAIM;
import static org.assertj.core.api.Assertions.assertThat;

class LightWeightTokenIntrospectionConditionTest {

    private final LightweightTokenIntrospectionCondition lightweightTokenIntrospectionCondition = new LightweightTokenIntrospectionCondition();
    private final AlwaysTokenIntrospectionCondition alwaysTokenIntrospectionCondition = new AlwaysTokenIntrospectionCondition();
    private final NeverTokenIntrospectionCondition neverTokenIntrospectionCondition = new NeverTokenIntrospectionCondition();

     @Test
     void testNeedsIntrospection_ifRolesHaveBeenPruned_ThenYes() {
         Jwt jwt = createJwt(null, 9000);
         assertConditions(jwt, true);
     }

    @Test
    void testNeedsIntrospection_ifLightweightScopeAtEnd_ThenYes() {
        Jwt jwt = createJwt("openid email lightweight", null);
        assertConditions(jwt, true);
    }

    @Test
    void testNeedsIntrospection_ifLightweightScopeAtStart_ThenYes() {
        Jwt jwt = createJwt("lightweight openid email", null);
        assertConditions(jwt, true);
    }

    @Test
    void testNeedsIntrospection_ifLightweightScopeInTheMiddle_ThenYes() {
        Jwt jwt = createJwt("openid lightweight email", null);
        assertConditions(jwt, true);
    }

    @Test
    void testNeedsIntrospection_ifLightweightScopeInTheMiddleWithAdditionalSpaces_ThenYes() {
        Jwt jwt = createJwt("openid    lightweight   email", null);
        assertConditions(jwt, true);
    }

    @Test
    void testNeedsIntrospection_ifLightweightScopeSingleScope_ThenYes() {
        Jwt jwt = createJwt("lightweight", null);
        assertConditions(jwt, true);
    }

    @Test
    void testNeedsIntrospection_ifNoRolesHaveBeenPrunedAndNoLightweightScope_ThenNo() {
        Jwt jwt = createJwt("openid", null);
        assertConditions(jwt, false);
    }

    @Test
    void testNeedsIntrospection_ifNoRolesHaveBeenPrunedAndEmptyStringScope_ThenNo() {
        Jwt jwt = createJwt("", null);
        assertConditions(jwt, false);
    }

    @Test
    void testNeedsIntrospection_ifNoRolesHaveBeenPrunedAndNoScope_ThenNo() {
        Jwt jwt = createJwt(null, null);
        assertConditions(jwt, false);
    }

    @Test
    void testNeedsIntrospection_ifRolesHaveBeenPrunedAndLightweightScope_ThenYes() {
        Jwt jwt = createJwt("openid email lightweight", 9000);
        assertConditions(jwt, true);
    }

    @Test
    void testNeedsIntrospection_ifScopesArrayWithLightweightScope_ThenYes() {
        Jwt jwt = createJwtWithScopeArray("openid", "lightweight", "email");
        assertConditions(jwt, true);
    }

    @Test
    void testNeedsIntrospection_ifScopesArrayWithoutLightweightScope_ThenNo() {
        Jwt jwt = createJwtWithScopeArray("openid", "email");
        assertConditions(jwt, false);
    }

    @Test
    void testNeedsIntrospection_ifScopesArrayEmpty_ThenNo() {
        Jwt jwt = createJwtWithScopeArray(); // empty scope array
        assertConditions(jwt, false);
    }

    private void assertConditions(Jwt jwt, boolean isLightweight) {
        assertThat(lightweightTokenIntrospectionCondition.needsIntrospection(jwt)).isEqualTo(isLightweight);
        assertThat(alwaysTokenIntrospectionCondition.needsIntrospection(jwt)).isTrue();
        assertThat(neverTokenIntrospectionCondition.needsIntrospection(jwt)).isFalse();
    }

     private Jwt createJwt(String scope, Integer prunedRolesChars) {
         var jwtBuilder = Jwt.withTokenValue("dummy")
                 .header("dummy-header", "dummy-value"); // at least one header required
         if (scope != null) {
             jwtBuilder.claim(OAuth2TokenIntrospectionClaimNames.SCOPE, scope);
         }
         if (prunedRolesChars != null) {
             jwtBuilder.claim(ROLES_PRUNED_CHARS_CLAIM, prunedRolesChars);
         }
         if ((scope == null) && (prunedRolesChars == null)) {
             jwtBuilder.claim("some_claim", "some_value"); // at least one claim required
         }
         return jwtBuilder.build();
     }

    private Jwt createJwtWithScopeArray(String... scopes) {
        var jwtBuilder = Jwt.withTokenValue("dummy")
                .header("dummy-header", "dummy-value"); // at least one header required
        if (scopes != null) {
            jwtBuilder.claim(OAuth2TokenIntrospectionClaimNames.SCOPE, scopes);
        } else {
            jwtBuilder.claim("some_claim", "some_value"); // at least one claim required
        }
        return jwtBuilder.build();
    }

}
