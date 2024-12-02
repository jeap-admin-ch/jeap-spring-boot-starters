package ch.admin.bit.jeap.security.test.resource;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ServletSimpleAuthorizationMockTest {

    // some business partner ids
    private static final String BP_1 = "bp1";
    private static final String BP_2 = "other";

    // some roles
    private static final String R_1 = "r1";
    private static final String R_2 = "r2";
    private static final String R_3 = "r3";
    private static final String R_4 = "r4";
    private static final String R_5 = "r5";

    @Test
    void testCreateMockWithUserRoles() {

        ServletSimpleAuthorizationMock mock = ServletSimpleAuthorizationMock.builder().userRoles(Set.of(R_1, R_2)).build();

        assertThat(mock.hasRoleForPartner(R_1, BP_1)).isTrue();
        assertThat(mock.hasRoleForPartner(R_2, BP_1)).isTrue();
        assertThat(mock.hasRoleForPartner(R_3, BP_1)).isFalse();
        assertThat(mock.hasRoleForPartner(R_1, BP_2)).isTrue();
        assertThat(mock.hasRoleForPartner(R_2, BP_2)).isTrue();
        assertThat(mock.hasRoleForPartner(R_3, BP_2)).isFalse();
        assertThat(mock.getJeapAuthenticationToken().getUserRoles()).containsOnly(R_1, R_2);
        assertThat(mock.getJeapAuthenticationToken().getBusinessPartnerRoles()).isEmpty();
    }

    @Test
    void testCreateMockWithBusinessPartnerRoles() {

        ServletSimpleAuthorizationMock mock = ServletSimpleAuthorizationMock.builder().businessPartnerRole(BP_1, Set.of(R_1, R_2)).build();

        assertThat(mock.hasRoleForPartner(R_1, BP_1)).isTrue();
        assertThat(mock.hasRoleForPartner(R_2, BP_1)).isTrue();
        assertThat(mock.hasRoleForPartner(R_3, BP_1)).isFalse();
        assertThat(mock.hasRoleForPartner(R_1, BP_2)).isFalse();
        assertThat(mock.hasRoleForPartner(R_2, BP_2)).isFalse();
        assertThat(mock.hasRoleForPartner(R_3, BP_2)).isFalse();
        assertThat(mock.getJeapAuthenticationToken().getUserRoles()).isEmpty();
        assertThat(mock.getJeapAuthenticationToken().getBusinessPartnerRoles()).containsOnlyKeys(BP_1);
        assertThat(mock.getJeapAuthenticationToken().getBusinessPartnerRoles().get(BP_1)).containsOnly(R_1, R_2);
    }

    @Test
    void testWithAuthentication() {
        JeapAuthenticationToken token =  JeapAuthenticationTestTokenBuilder.create().
                withUserRoles(R_1, R_2).
                withBusinessPartnerRoles(BP_1, R_3, R_4).
                build();

        ServletSimpleAuthorizationMock mock = new ServletSimpleAuthorizationMock(token);

        assertThat(mock.hasRoleForPartner(R_1, BP_1)).isTrue();
        assertThat(mock.hasRoleForPartner(R_2, BP_1)).isTrue();
        assertThat(mock.hasRoleForPartner(R_3, BP_1)).isTrue();
        assertThat(mock.hasRoleForPartner(R_4, BP_1)).isTrue();
        assertThat(mock.hasRoleForPartner(R_5, BP_1)).isFalse();
        assertThat(mock.hasRoleForPartner(R_1, BP_2)).isTrue();
        assertThat(mock.hasRoleForPartner(R_2, BP_2)).isTrue();
        assertThat(mock.hasRoleForPartner(R_3, BP_2)).isFalse();
        assertThat(mock.hasRoleForPartner(R_4, BP_2)).isFalse();
        assertThat(mock.hasRoleForPartner(R_5, BP_2)).isFalse();
        assertThat(mock.getJeapAuthenticationToken().getUserRoles()).containsOnly(R_1, R_2);
        assertThat(mock.getJeapAuthenticationToken().getBusinessPartnerRoles()).containsOnlyKeys(BP_1);
        assertThat(mock.getJeapAuthenticationToken().getBusinessPartnerRoles().get(BP_1)).containsOnly(R_3, R_4);
    }

    @Test
    void testSetJeapAuthenticationToken() {
        final JeapAuthenticationToken userroleToken =  JeapAuthenticationTestTokenBuilder.create().
                withBusinessPartnerRoles(BP_1, R_3, R_4).
                build();
        final JeapAuthenticationToken businessPartnerRoleToken =  JeapAuthenticationTestTokenBuilder.create().
                withBusinessPartnerRoles(BP_1, R_3, R_4).
                build();
        ServletSimpleAuthorizationMock mock = new ServletSimpleAuthorizationMock();

        // Set token to base authorization on
        mock.setJeapAuthenticationToken(userroleToken);

        assertThat(mock.getJeapAuthenticationToken()).isEqualTo(userroleToken);

        // Change token to base authorization on
        mock.setJeapAuthenticationToken(businessPartnerRoleToken);

        assertThat(mock.getJeapAuthenticationToken()).isEqualTo(businessPartnerRoleToken);
    }


}
