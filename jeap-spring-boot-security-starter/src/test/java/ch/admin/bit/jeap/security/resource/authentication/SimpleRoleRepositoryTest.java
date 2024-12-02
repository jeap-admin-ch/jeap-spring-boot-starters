package ch.admin.bit.jeap.security.resource.authentication;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

class SimpleRoleRepositoryTest {

    private static final String BP1 = "business partner 1";
    private static final String BP2 = "business partner 2";
    private static final String BP3 = "business partner 3"; // not granted any roles

    private static final String R1 = "role 1";
    private static final String R2 = "role 2";
    private static final String R3 = "role 3";
    private static final String R4 = "role 4";
    private static final String R5 = "role 5";
    private static final String R6 = "role 6"; // not granted at all

    private static final SimpleRoleRepository SIMPLE_ROLE_REPOSITORY = new SimpleRoleRepository(
            Set.of(R1, R5),
            Map.of(BP1, Set.of(R2, R3),
                   BP2, Set.of(R3, R4)));

    @Test
    void hasRole() {
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRole(R1)).isTrue();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRole(R2)).isTrue();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRole(R3)).isTrue();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRole(R4)).isTrue();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRole(R5)).isTrue();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRole(R6)).isFalse();
    }

    @Test
    void testHasRoleForPartner() {
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R1, BP1)).isTrue();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R2, BP1)).isTrue();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R3, BP1)).isTrue();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R4, BP1)).isFalse();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R5, BP1)).isTrue();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R6, BP1)).isFalse();

        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R1, BP2)).isTrue();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R2, BP2)).isFalse();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R3, BP2)).isTrue();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R4, BP2)).isTrue();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R5, BP2)).isTrue();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R6, BP2)).isFalse();

        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R1, BP3)).isTrue();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R2, BP3)).isFalse();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R3, BP3)).isFalse();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R4, BP3)).isFalse();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R5, BP3)).isTrue();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForPartner(R6, BP3)).isFalse();
    }

    @Test
    void testHasRoleForAllBusinessPartners() {
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForAllPartners(R1)).isTrue();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForAllPartners(R2)).isFalse();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForAllPartners(R3)).isFalse();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForAllPartners(R4)).isFalse();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForAllPartners(R5)).isTrue();
        assertThat(SIMPLE_ROLE_REPOSITORY.hasRoleForAllPartners(R6)).isFalse();
    }

    @Test
    void testGetBusinessPartnersForRole() {
        assertThat(SIMPLE_ROLE_REPOSITORY.getPartnersForRole(R1)).isEmpty();
        assertThat(SIMPLE_ROLE_REPOSITORY.getPartnersForRole(R2)).containsOnly(BP1);
        assertThat(SIMPLE_ROLE_REPOSITORY.getPartnersForRole(R3)).containsOnly(BP1, BP2);
        assertThat(SIMPLE_ROLE_REPOSITORY.getPartnersForRole(R4)).containsOnly(BP2);
        assertThat(SIMPLE_ROLE_REPOSITORY.getPartnersForRole(R5)).isEmpty();
        assertThat(SIMPLE_ROLE_REPOSITORY.getPartnersForRole(R6)).isEmpty();
    }

    @Test
    void testGetUserRoles() {
        assertThat(SIMPLE_ROLE_REPOSITORY.getUserRoles()).containsOnly(R1, R5);
    }

    @Test
    void testBusinessPartnerRoles() {
        assertThat(SIMPLE_ROLE_REPOSITORY.getBusinessPartnerRoles()).containsOnly(
                entry(BP1, Set.of(R2, R3)),
                entry(BP2, Set.of(R3, R4)));
    }

}
