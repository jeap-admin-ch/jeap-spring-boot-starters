package ch.admin.bit.jeap.security.test.resource;

import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.ServletSemanticAuthorization;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ServletSemanticAuthorizationMockTest {

    private static final String SYSTEM = "system";
    private static final String RESOURCE = "resource";

    // some business partner ids
    private static final String BP_1 = "bp1";
    private static final String BP_2 = "other";


    // some roles
    private static final SemanticApplicationRole R_1 = SemanticApplicationRole.builder()
            .system(SYSTEM)
            .operation("r1")
            .resource(RESOURCE)
            .build();
    private static final SemanticApplicationRole R_2 = SemanticApplicationRole.builder()
            .system(SYSTEM)
            .operation("r2")
            .resource(RESOURCE)
            .build();
    private static final SemanticApplicationRole R_3 = SemanticApplicationRole.builder()
            .system(SYSTEM)
            .operation("r3")
            .resource(RESOURCE)
            .build();
    private static final SemanticApplicationRole R_4 = SemanticApplicationRole.builder()
            .system(SYSTEM)
            .operation("r4")
            .resource(RESOURCE)
            .build();
    private static final SemanticApplicationRole R_5 = SemanticApplicationRole.builder()
            .system(SYSTEM)
            .operation("r5")
            .resource(RESOURCE)
            .build();

    @Test
    void testCreateMockWithUserRoles() {
        ServletSemanticAuthorization mock = ServletSemanticAuthorizationMock.builder()
                .systemName(SYSTEM)
                .userRole(R_1)
                .userRole(R_2)
                .build();

        assertThat(mock.hasRoleForPartner(R_1, BP_1)).isTrue();
        assertThat(mock.hasRoleForPartner(R_2, BP_1)).isTrue();
        assertThat(mock.hasRoleForPartner(R_3, BP_1)).isFalse();
        assertThat(mock.hasRoleForPartner(R_1, BP_2)).isTrue();
        assertThat(mock.hasRoleForPartner(R_2, BP_2)).isTrue();
        assertThat(mock.hasRoleForPartner(R_3, BP_2)).isFalse();
        JeapAuthenticationToken authenticationToken = mock.getAuthenticationToken();
        assertThat(authenticationToken.getUserRoles()).containsOnly(R_1.toString(), R_2.toString());
        assertThat(authenticationToken.getBusinessPartnerRoles()).isEmpty();
    }

    @Test
    void testCreateMockWithBusinessPartnerRoles() {
        ServletSemanticAuthorization mock = ServletSemanticAuthorizationMock.builder()
                .systemName(SYSTEM)
                .businessPartnerRole(BP_1, Set.of(R_1, R_2))
                .build();

        assertThat(mock.hasRoleForPartner(R_1, BP_1)).isTrue();
        assertThat(mock.hasRoleForPartner(R_2, BP_1)).isTrue();
        assertThat(mock.hasRoleForPartner(R_3, BP_1)).isFalse();
        assertThat(mock.hasRoleForPartner(R_1, BP_2)).isFalse();
        assertThat(mock.hasRoleForPartner(R_2, BP_2)).isFalse();
        assertThat(mock.hasRoleForPartner(R_3, BP_2)).isFalse();
        JeapAuthenticationToken authenticationToken = mock.getAuthenticationToken();
        assertThat(authenticationToken.getUserRoles()).isEmpty();
        assertThat(authenticationToken.getBusinessPartnerRoles()).containsOnlyKeys(BP_1);
        assertThat(authenticationToken.getBusinessPartnerRoles().get(BP_1)).containsOnly(R_1.toString(), R_2.toString());
    }

    @Test
    void testWithAuthentication() {
        ServletSemanticAuthorization mock = ServletSemanticAuthorizationMock.builder()
                .systemName(SYSTEM)
                .userRole(R_1)
                .userRole(R_2)
                .businessPartnerRole(BP_1, Set.of(R_3, R_4))
                .build();

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
        JeapAuthenticationToken authenticationToken = mock.getAuthenticationToken();
        assertThat(authenticationToken.getUserRoles()).containsOnly(R_1.toString(), R_2.toString());
        assertThat(authenticationToken.getBusinessPartnerRoles()).containsOnlyKeys(BP_1);
        assertThat(authenticationToken.getBusinessPartnerRoles().get(BP_1)).containsOnly(R_3.toString(), R_4.toString());
    }

    @Test
    void testSetJeapAuthenticationToken() {
        final JeapAuthenticationToken userroleToken = JeapAuthenticationTestTokenBuilder.create().
                withBusinessPartnerRoles(BP_1, R_3.toString(), R_4.toString()).
                build();
        final JeapAuthenticationToken businessPartnerRoleToken = JeapAuthenticationTestTokenBuilder.create().
                withBusinessPartnerRoles(BP_1, R_3.toString(), R_4.toString()).
                build();
        ServletSemanticAuthorizationMock mock = new ServletSemanticAuthorizationMock(SYSTEM);

        // Set token to base authorization on
        mock.setAuthenticationToken(userroleToken);

        assertThat(mock.getAuthenticationToken()).isEqualTo(userroleToken);

        // Change token to base authorization on
        mock.setAuthenticationToken(businessPartnerRoleToken);

        assertThat(mock.getAuthenticationToken()).isEqualTo(businessPartnerRoleToken);
    }


}
