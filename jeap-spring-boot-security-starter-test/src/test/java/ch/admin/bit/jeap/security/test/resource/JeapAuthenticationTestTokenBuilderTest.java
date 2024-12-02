package ch.admin.bit.jeap.security.test.resource;

import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JeapAuthenticationTestTokenBuilderTest {

    // some user roles
    private static final String UR_1 = "ur1";
    private static final String UR_2 = "ur2";
    private static final String UR_3 = "ur3";

    // some business partner ids
    private static final String BP_1 = "bp1";
    private static final String BP_2 = "bp2";
    private static final String BP_3 = "bp3";
    private static final String BP_4 = "bp4";

    // some business partner roles
    private static final String BPR_1 = "bpr1";
    private static final String BPR_2 = "bpr2";
    private static final String BPR_3 = "bpr3";
    private static final String BPR_4 = "bpr4";

    // some semantic roles
    private static final SemanticApplicationRole SR_1 = SemanticApplicationRole.builder().
            system("jeap").
            resource("thing").
            operation("o1").
            build();
    private static final SemanticApplicationRole SR_2 = SemanticApplicationRole.builder().
            system("jeap").
            resource("thing").
            operation("o2").
            build();
    private static final SemanticApplicationRole SR_3 = SemanticApplicationRole.builder().
            system("jeap").
            resource("thing").
            operation("o3").
            build();
    private static final SemanticApplicationRole SR_4 = SemanticApplicationRole.builder().
            system("jeap").
            resource("thing").
            operation("o4").
            build();


    // some claim names and values
    private static final String SUB_CLAIM = "sub";
    private static final String SUB_ID = "sid";

    // some user data
    private static final String EXT_ID = "287365";
    private static final String NAME = "Max Muster";
    private static final String GIVEN_NAME = "Max";
    private static final String FAMILY_NAME = "Muster";
    private static final String PREFERRED_USERNAME = "max_muster";
    private static final String LOCALE_DE = "DE";
    private static final String SUBJECT = "69368608-D736-43C8-5F76-55B7BF168299";
    private static final String CLAIM_NAME = "claim name";
    private static final String CLAIM_VALUE = "claim value";
    private static final String ADMIN_DIR_UID = "U11111111";

    // some technical token data
    private static final JeapAuthenticationContext USER_CONTEXT = JeapAuthenticationContext.USER;

    @Test
    void testBuildWithoutConfiguration() {
        JeapAuthenticationToken jeapAuthtoken = JeapAuthenticationTestTokenBuilder.create().build();

        assertThat(jeapAuthtoken.getUserRoles()).isEmpty();
        assertThat(jeapAuthtoken.getBusinessPartnerRoles()).isEmpty();
        assertThat(jeapAuthtoken.getToken()).isNotNull();
    }

    @Test
    void testBuildWithConfiguration() {
        JeapAuthenticationToken jeapAuthToken = JeapAuthenticationTestTokenBuilder.create().
                withUserRoles(UR_1, UR_2).
                withBusinessPartnerRoles(BP_1, BPR_1, BPR_2).
                withBusinessPartnerRoles(BP_2, BPR_1, BPR_3).
                withUserRoles(UR_3).
                withBusinessPartnerRoles(BP_2, BPR_4).
                withUserRoles(SR_1, SR_3).
                withBusinessPartnerRoles(BP_3, SR_1, SR_3).
                withUserRoles(SR_2).
                withBusinessPartnerRoles(BP_3, SR_2).
                withBusinessPartnerRoles(BP_4, SR_4).
                withExtId(EXT_ID).
                withAdminDirUID(ADMIN_DIR_UID).
                withName(NAME).
                withGivenName(GIVEN_NAME).
                withFamilyName(FAMILY_NAME).
                withPreferredUsername(PREFERRED_USERNAME).
                withLocale(LOCALE_DE).
                withSubject(SUBJECT).
                withClaim(CLAIM_NAME, CLAIM_VALUE).
                withContext(USER_CONTEXT).
                build();

        assertThat(jeapAuthToken.getUserRoles()).containsOnly(UR_1, UR_2, UR_3, SR_1.toString(), SR_2.toString(), SR_3.toString());
        assertThat(jeapAuthToken.getBusinessPartnerRoles()).containsOnlyKeys(BP_1, BP_2, BP_3, BP_4);
        assertThat(jeapAuthToken.getBusinessPartnerRoles().get(BP_1)).containsOnly(BPR_1, BPR_2);
        assertThat(jeapAuthToken.getBusinessPartnerRoles().get(BP_2)).containsOnly(BPR_1, BPR_3, BPR_4);
        assertThat(jeapAuthToken.getBusinessPartnerRoles().get(BP_3)).containsOnly(SR_1.toString(), SR_2.toString(), SR_3.toString());
        assertThat(jeapAuthToken.getBusinessPartnerRoles().get(BP_4)).containsOnly(SR_4.toString());
        assertThat(jeapAuthToken.getTokenExtId()).isEqualTo(EXT_ID);
        assertThat(jeapAuthToken.getAdminDirUID()).isEqualTo(ADMIN_DIR_UID);
        assertThat(jeapAuthToken.getTokenName()).isEqualTo(NAME);
        assertThat(jeapAuthToken.getTokenGivenName()).isEqualTo(GIVEN_NAME);
        assertThat(jeapAuthToken.getTokenFamilyName()).isEqualTo(FAMILY_NAME);
        assertThat(jeapAuthToken.getPreferredUsername()).isEqualTo(PREFERRED_USERNAME);
        assertThat(jeapAuthToken.getTokenLocale()).isEqualTo(LOCALE_DE);
        assertThat(jeapAuthToken.getTokenSubject()).isEqualTo(SUBJECT);
        assertThat(jeapAuthToken.getToken().getClaimAsString(CLAIM_NAME)).isEqualTo(CLAIM_VALUE);
        assertThat(jeapAuthToken.getJeapAuthenticationContext()).isEqualTo(USER_CONTEXT);
    }

    @Test
    void testBuildWithJwtOnly() {
        Jwt jwt = createJwt();

        JeapAuthenticationToken jeapAuthToken = JeapAuthenticationTestTokenBuilder.createWithJwt(jwt).build();

        assertThat(jeapAuthToken.getToken()).isEqualTo(jwt);
    }

    @Test
    void testBuildWithJwtAndConfiguration() {
        Jwt jwt = createJwt();

        JeapAuthenticationToken jeapAuthToken = JeapAuthenticationTestTokenBuilder.createWithJwt(jwt).
                withUserRoles(UR_1).
                withUserRoles(SR_1).
                withBusinessPartnerRoles(BP_1, BPR_1).
                withBusinessPartnerRoles(BP_1, SR_2).
                build();

        assertThat(jeapAuthToken.getToken()).isEqualTo(jwt);
        assertThat(jeapAuthToken.getUserRoles()).containsOnly(UR_1, SR_1.toString());
        assertThat(jeapAuthToken.getBusinessPartnerRoles()).containsOnlyKeys(BP_1);
        assertThat(jeapAuthToken.getBusinessPartnerRoles().get(BP_1)).containsOnly(BPR_1, SR_2.toString());
    }

    @Test
    void testBuildWithCustomAuthorities() {
        JeapAuthenticationToken token = JeapAuthenticationTestTokenBuilder.create()
                .withAuthorities("authority1", "authority2")
                .build();

        assertThat(token.getAuthorities()).hasSize(2);
        Set<String> authorityStrings = authorityStrings(token);
        assertThat(authorityStrings).containsExactlyInAnyOrder("authority1", "authority2");
    }

    @Test
    void testBuildWithDefaultAuthorities() {
        JeapAuthenticationToken token = JeapAuthenticationTestTokenBuilder.create()
                .withUserRoles("user", "admin", "superadmin")
                .build();

        assertThat(token.getAuthorities()).hasSize(3);
        Set<String> authorityStrings = authorityStrings(token);
        assertThat(authorityStrings).containsExactlyInAnyOrder("ROLE_user", "ROLE_admin", "ROLE_superadmin");
    }

    private Set<String> authorityStrings(JeapAuthenticationToken token) {
        return token.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    private Jwt createJwt() {
        return Jwt.withTokenValue("test-token-value").
                header("test-header", "some-header-value"). // at least one header needed
                        claim(JeapAuthenticationContext.getContextJwtClaimName(), JeapAuthenticationContext.SYS.name()). // at least one claim needed
                        claim(SUB_CLAIM, SUB_ID).
                build();
    }

}
