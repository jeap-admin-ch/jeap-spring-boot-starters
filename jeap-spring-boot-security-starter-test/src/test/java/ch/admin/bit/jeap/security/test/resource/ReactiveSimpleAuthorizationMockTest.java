package ch.admin.bit.jeap.security.test.resource;

import ch.admin.bit.jeap.security.resource.authentication.SimpleRoleRepository;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.resource.authentication.ReactiveSimpleAuthorization;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ReactiveSimpleAuthorizationMockTest {

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

        ReactiveSimpleAuthorization mock = ReactiveSimpleAuthorizationMock.builder().userRoles(Set.of(R_1, R_2)).build();

        SimpleRoleRepository roleRepo = mock.getSimpleRoleRepository().block();
        assertThat(roleRepo.hasRoleForPartner(R_1, BP_1)).isTrue();
        assertThat(roleRepo.hasRoleForPartner(R_2, BP_1)).isTrue();
        assertThat(roleRepo.hasRoleForPartner(R_3, BP_1)).isFalse();
        assertThat(roleRepo.hasRoleForPartner(R_1, BP_2)).isTrue();
        assertThat(roleRepo.hasRoleForPartner(R_2, BP_2)).isTrue();
        assertThat(roleRepo.hasRoleForPartner(R_3, BP_2)).isFalse();
        assertThat(mock.getJeapAuthenticationToken().block().getUserRoles()).containsOnly(R_1, R_2);
        assertThat(mock.getJeapAuthenticationToken().block().getBusinessPartnerRoles()).isEmpty();
    }

    @Test
    void testCreateMockWithBusinessPartnerRoles() {

        ReactiveSimpleAuthorization mock = ReactiveSimpleAuthorizationMock.builder().businessPartnerRole(BP_1, Set.of(R_1, R_2)).build();

        SimpleRoleRepository roleRepo = mock.getSimpleRoleRepository().block();
        assertThat(roleRepo.hasRoleForPartner(R_1, BP_1)).isTrue();
        assertThat(roleRepo.hasRoleForPartner(R_2, BP_1)).isTrue();
        assertThat(roleRepo.hasRoleForPartner(R_3, BP_1)).isFalse();
        assertThat(roleRepo.hasRoleForPartner(R_1, BP_2)).isFalse();
        assertThat(roleRepo.hasRoleForPartner(R_2, BP_2)).isFalse();
        assertThat(roleRepo.hasRoleForPartner(R_3, BP_2)).isFalse();
        assertThat(mock.getJeapAuthenticationToken().block().getUserRoles()).isEmpty();
        assertThat(mock.getJeapAuthenticationToken().block().getBusinessPartnerRoles()).containsOnlyKeys(BP_1);
        assertThat(mock.getJeapAuthenticationToken().block().getBusinessPartnerRoles().get(BP_1)).containsOnly(R_1, R_2);
    }

    @Test
    void testWithAuthentication() {
        JeapAuthenticationToken token =  JeapAuthenticationTestTokenBuilder.create().
                withUserRoles(R_1, R_2).
                withBusinessPartnerRoles(BP_1, R_3, R_4).
                build();

        ReactiveSimpleAuthorizationMock mock = new ReactiveSimpleAuthorizationMock(token);

        SimpleRoleRepository roleRepo = mock.getSimpleRoleRepository().block();
        assertThat(roleRepo.hasRoleForPartner(R_1, BP_1)).isTrue();
        assertThat(roleRepo.hasRoleForPartner(R_2, BP_1)).isTrue();
        assertThat(roleRepo.hasRoleForPartner(R_3, BP_1)).isTrue();
        assertThat(roleRepo.hasRoleForPartner(R_4, BP_1)).isTrue();
        assertThat(roleRepo.hasRoleForPartner(R_5, BP_1)).isFalse();
        assertThat(roleRepo.hasRoleForPartner(R_1, BP_2)).isTrue();
        assertThat(roleRepo.hasRoleForPartner(R_2, BP_2)).isTrue();
        assertThat(roleRepo.hasRoleForPartner(R_3, BP_2)).isFalse();
        assertThat(roleRepo.hasRoleForPartner(R_4, BP_2)).isFalse();
        assertThat(roleRepo.hasRoleForPartner(R_5, BP_2)).isFalse();
        assertThat(mock.getJeapAuthenticationToken().block().getUserRoles()).containsOnly(R_1, R_2);
        assertThat(mock.getJeapAuthenticationToken().block().getBusinessPartnerRoles()).containsOnlyKeys(BP_1);
        assertThat(mock.getJeapAuthenticationToken().block().getBusinessPartnerRoles().get(BP_1)).containsOnly(R_3, R_4);
    }

    @Test
    void testSetJeapAuthenticationToken() {
        final JeapAuthenticationToken userroleToken =  JeapAuthenticationTestTokenBuilder.create().
                withBusinessPartnerRoles(BP_1, R_3, R_4).
                build();
        final JeapAuthenticationToken businessPartnerRoleToken =  JeapAuthenticationTestTokenBuilder.create().
                withBusinessPartnerRoles(BP_1, R_3, R_4).
                build();
        ReactiveSimpleAuthorizationMock mock = new ReactiveSimpleAuthorizationMock();

        // Set token to base authorization on
        mock.setJeapAuthenticationToken(userroleToken);

        assertThat(mock.getJeapAuthenticationToken().block()).isEqualTo(userroleToken);

        // Change token to base authorization on
        mock.setJeapAuthenticationToken(businessPartnerRoleToken);

        assertThat(mock.getJeapAuthenticationToken().block()).isEqualTo(businessPartnerRoleToken);
    }

}
