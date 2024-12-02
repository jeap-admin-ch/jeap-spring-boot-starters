package ch.admin.bit.jeap.security.resource.semanticAuthentication;

import ch.admin.bit.jeap.security.resource.token.DefaultAuthoritiesResolver;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationConverter;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SemanticRoleRepositoryTest {
    private static final String SYSTEM = "system";
    private static final String PARTNER = "partner";
    private static final String OTHER_PARTNER = "other-partner";

    private static final String USER_ROLES_CLAIM = "userroles";
    private static final String BUSINESS_PARTNER_ROLES_CLAIM = "bproles";
    private final static SemanticApplicationRole READ = SemanticApplicationRole.builder()
            .system(SYSTEM)
            .resource("test")
            .operation("read")
            .build();
    private final static SemanticApplicationRole WRITE = SemanticApplicationRole.builder()
            .system(SYSTEM)
            .resource("test")
            .operation("write")
            .build();
    private final static SemanticApplicationRole DELETE = SemanticApplicationRole.builder()
            .system(SYSTEM)
            .resource("test")
            .operation("delete")
            .build();
    private final static SemanticApplicationRole OTHER_SYSTEM_READ = SemanticApplicationRole.builder()
            .system("other")
            .resource(READ.getResource())
            .operation(READ.getOperation())
            .build();
    private final static SemanticApplicationRole OTHER_SYSTEM_WRITE = SemanticApplicationRole.builder()
            .system("other")
            .resource(READ.getResource())
            .operation(READ.getOperation())
            .build();
    private static JeapAuthenticationToken token;

    @BeforeAll
    static void initialize() {
        token = getToken(List.of("system_#read"), Map.of(PARTNER, List.of("system_@test_#write")));
    }

    @Test
    void hasRoleForPartner() {
        SemanticRoleRepository target = new SemanticRoleRepository(SYSTEM, token);

        assertTrue(target.hasRoleForPartner(WRITE, PARTNER), "This role is given for this partner");
        assertTrue(target.hasRoleForPartner("test", "write", PARTNER), "This role is given for this partner");
        assertTrue(target.hasRoleForPartner("write", PARTNER), "This role is given for this partner");
        assertFalse(target.hasRoleForPartner(WRITE, "partner2"), "This role is not given for this partner");
        assertFalse(target.hasRoleForPartner("test", "write", "partner2"), "This role is not given for this partner");
        assertFalse(target.hasRoleForPartner("write", "partner2"), "This role is not given for this partner");
        assertTrue(target.hasRoleForPartner(READ, PARTNER), "This role is given for all partners");
        assertTrue(target.hasRoleForPartner("test", "read", PARTNER), "This role is given for all partners");
        assertTrue(target.hasRoleForPartner( "read", PARTNER), "This role is given for all partners");
        assertFalse(target.hasRoleForPartner(DELETE, "partner2"), "This role is not given for any partner");
        assertFalse(target.hasRoleForPartner("test", "delete", "partner2"), "This role is not given for any partner");
        assertFalse(target.hasRoleForPartner("delete", "partner2"), "This role is not given for any partner");
    }

    @Test
    void hasRole() {
        SemanticRoleRepository target = new SemanticRoleRepository(SYSTEM, token);

        assertTrue(target.hasRole(WRITE), "This role is given for a partner");
        assertTrue(target.hasRole("test", "write"), "This role is given for a partner");
        assertTrue(target.hasOperation("write"), "This role is given for a partner");
        assertTrue(target.hasRole(READ), "This role is given for all partners");
        assertTrue(target.hasRole("test", "read"), "This role is given for all partners");
        assertTrue(target.hasOperation("read"), "This role is given for all partners");
        assertFalse(target.hasRole(DELETE), "This role is not given for any partner");
        assertFalse(target.hasRole("test", "delete"), "This role is not given for any partner");
        assertFalse(target.hasOperation("delete"), "This role is not given for any partner");
    }

    @Test
    void hasRoleForAllPartners() {
        SemanticRoleRepository target = new SemanticRoleRepository(SYSTEM, token);

        assertFalse(target.hasRoleForAllPartners("test", "write"), "This role is only given for a partner");
        assertFalse(target.hasRoleForAllPartners( "write"), "This role is only given for a partner");
        assertFalse(target.hasRoleForAllPartners(WRITE), "This role is only given for a partner");
        assertTrue(target.hasRoleForAllPartners("test", "read"), "This role is given for all partners");
        assertTrue(target.hasRoleForAllPartners("read"), "This role is given for all partners");
        assertTrue(target.hasRoleForAllPartners(READ), "This role is given for all partners");
        assertFalse(target.hasRoleForAllPartners("test", "delete"), "This role is not given for any partner");
        assertFalse(target.hasRoleForAllPartners("delete"), "This role is not given for any partner");
        assertFalse(target.hasRoleForAllPartners(DELETE), "This role is not given for any partner");
    }

    @Test
    void getAllRolesForPartner_singleRole() {
        SemanticRoleRepository target = new SemanticRoleRepository(SYSTEM, token);

        //This role is given only to partner
        Collection<SemanticApplicationRole> resultsWritePartner = target.getAllRolesForPartner("write", PARTNER);
        assertEquals(1, resultsWritePartner.size());
        assertEquals(
                new SemanticApplicationRole(SYSTEM, null, "test", "write"),
                ((List<SemanticApplicationRole>) resultsWritePartner).get(0));
        Collection<SemanticApplicationRole> resultsWritePartner2 = target.getAllRolesForPartner("write", "partner2");
        assertEquals(0, resultsWritePartner2.size());

        //This role is given to all partners and is a wildcard
        Collection<SemanticApplicationRole> resultsReadPartner = target.getAllRolesForPartner("read", PARTNER);
        assertEquals(1, resultsReadPartner.size());
        assertEquals(
                new SemanticApplicationRole(SYSTEM, null, null, "read"),
                ((List<SemanticApplicationRole>) resultsReadPartner).get(0));
        Collection<SemanticApplicationRole> resultsReadPartner2 = target.getAllRolesForPartner("read", "partner2");
        assertEquals(1, resultsReadPartner2.size());
        assertEquals(
                new SemanticApplicationRole(SYSTEM, null, null, "read"),
                ((List<SemanticApplicationRole>) resultsReadPartner2).get(0));

        //This role is not given to any partner
        Collection<SemanticApplicationRole> resultsDeletePartner = target.getAllRolesForPartner("delete", PARTNER);
        assertEquals(0, resultsDeletePartner.size());
        Collection<SemanticApplicationRole> resultsDeletePartner2 = target.getAllRolesForPartner("delete", "partner2");
        assertEquals(0, resultsDeletePartner2.size());
    }

    @Test
    void getAllRoles() {
        SemanticRoleRepository target = new SemanticRoleRepository(SYSTEM, token);

        //This role is given to one partner
        Collection<SemanticApplicationRole> resultsWrite = target.getAllRoles("write");
        assertEquals(1, resultsWrite.size());
        assertEquals(
                new SemanticApplicationRole(SYSTEM, null, "test", "write"),
                ((List<SemanticApplicationRole>) resultsWrite).get(0));

        //This role is given to all partners and is a wildcard
        Collection<SemanticApplicationRole> resultsRead = target.getAllRoles("read");
        assertEquals(1, resultsRead.size());
        assertEquals(
                new SemanticApplicationRole(SYSTEM, null, null, "read"),
                ((List<SemanticApplicationRole>) resultsRead).get(0));

        //This role is not given to any partner
        Collection<SemanticApplicationRole> resultsDelete = target.getAllRoles("delete");
        assertEquals(0, resultsDelete.size());
    }


    @Test
    void getAllRolesForAllPartners() {
        SemanticRoleRepository target = new SemanticRoleRepository(SYSTEM, token);

        //This role is given to one partner
        Collection<SemanticApplicationRole> resultsWrite = target.getAllRolesForAllPartners("write");
        assertEquals(0, resultsWrite.size());

        //This role is given to all partners and is a wildcard
        Collection<SemanticApplicationRole> resultsRead = target.getAllRolesForAllPartners("read");
        assertEquals(1, resultsRead.size());
        assertEquals(
                new SemanticApplicationRole(SYSTEM, null, null, "read"),
                ((List<SemanticApplicationRole>) resultsRead).get(0));

        //This role is not given to any partner
        Collection<SemanticApplicationRole> resultsDelete = target.getAllRolesForAllPartners("delete");
        assertEquals(0, resultsDelete.size());
    }

    @Test
    void testGetPartnersForRole() {
        JeapAuthenticationToken jeapAuthenticationToken = getToken(emptyList(), Map.of(
                "bp1", List.of("system_%t1_@r1_#o1", "system_%t1_@r1_#o2"),
                "bp2", List.of("system_%t1_@r1_#o1"),
                "bp3", List.of("system_%t2_@r1_#o1", "system_%t2_@r1_#o2"),
                "bp4", List.of("system_%t2_@r1_#o1"),
                "bp5", List.of("system_%t1_@r2_#o1", "system_%t1_@r2_#o2"),
                "bp6", List.of("system_%t1_@r2_#o1"),
                "bp7", List.of("system_@r1_#o1", "system_@r1_#o2"),
                "bp8", List.of("system_@r1_#o1"),
                "bp9", List.of("system_@r2_#o1", "system_@r2_#o2"),
                "bp10", List.of("system_@r2_#o1")));
        assertNotNull(jeapAuthenticationToken);

        SemanticRoleRepository target = new SemanticRoleRepository(SYSTEM, jeapAuthenticationToken);

        assertThat(target.getPartnersForRole("t1", "r1", "o1")).containsOnly("bp1", "bp2", "bp7", "bp8");
        assertThat(target.getPartnersForRole("t1", "r1", "o2")).containsOnly("bp1", "bp7");
        assertThat(target.getPartnersForRole("t1", "r2", "o1")).containsOnly("bp5", "bp6", "bp9", "bp10");
        assertThat(target.getPartnersForRole("t1", "r2", "o2")).containsOnly("bp5", "bp9");
        assertThat(target.getPartnersForRole("t2", "r1", "o1")).containsOnly("bp3", "bp4", "bp7", "bp8");
        assertThat(target.getPartnersForRole("t2", "r1", "o2")).containsOnly("bp3", "bp7");
        assertThat(target.getPartnersForRole("t2", "r2", "o1")).containsOnly("bp9", "bp10");
        assertThat(target.getPartnersForRole("r1", "o1")).containsOnly("bp1", "bp2", "bp3", "bp4", "bp7", "bp8");
        assertThat(target.getPartnersForRole("r1", "o2")).containsOnly("bp1", "bp3", "bp7");
        assertThat(target.getPartnersForRole("r2", "o1")).containsOnly("bp5", "bp6", "bp9", "bp10");
        assertThat(target.getPartnersForRole("r2", "o2")).containsOnly("bp5", "bp9");
        assertThat(target.getPartnersForRole("o1")).containsOnly("bp1", "bp2", "bp3", "bp4", "bp5", "bp6", "bp7", "bp8", "bp9", "bp10");
        assertThat(target.getPartnersForRole("o2")).containsOnly("bp1", "bp3", "bp5", "bp7", "bp9");
        assertThat(target.getPartnersForRole("t3", "r1", "o1")).containsOnly("bp7", "bp8");
        assertThat(target.getPartnersForRole("t1", "r3", "o1")).isEmpty();
        assertThat(target.getPartnersForRole("t1", "r1", "o3")).isEmpty();

        assertThat(target.getPartnersForRole(r -> r.sameOperationOrWildcard("o2"))).containsOnly("bp1", "bp3", "bp5", "bp7", "bp9");
        assertThat(target.getPartnersForRole(r -> r.sameOperationOrWildcard("o3"))).isEmpty();
    }

    @Test
    void testGetPartnersForRoleWithPredicateAndOtherSystem() {
        final JeapAuthenticationToken token = getToken(emptyList(), Map.of(
                PARTNER, List.of(READ.toString()),
                OTHER_PARTNER, List.of(OTHER_SYSTEM_READ.toString())));

        SemanticRoleRepository semanticRoleRepository = new SemanticRoleRepository(SYSTEM, token);

        Collection<String> partners = semanticRoleRepository.getPartnersForRole(role -> true);
        assertThat(partners).containsOnly(PARTNER);
    }

    @Test
    void testGetRolesMethods() {
        final JeapAuthenticationToken token = getToken(List.of(READ.toString(), OTHER_SYSTEM_READ.toString()),
                                                       Map.of(PARTNER, List.of(WRITE.toString(), OTHER_SYSTEM_WRITE.toString())));

        SemanticRoleRepository semanticRoleRepository = new SemanticRoleRepository(SYSTEM, token);

        assertThat(semanticRoleRepository.getUserRoles()).containsOnly(READ);
        assertThat(semanticRoleRepository.getBusinessPartnerRoles()).containsOnlyKeys(PARTNER);
        assertThat(semanticRoleRepository.getBusinessPartnerRoles().get(PARTNER)).containsOnly(WRITE);

        assertThat(semanticRoleRepository.getAllRoles(READ.getOperation())).containsOnly(READ);
        assertThat(semanticRoleRepository.getAllRoles(WRITE.getOperation())).containsOnly(WRITE);
        assertThat(semanticRoleRepository.getAllRolesForPartner(READ.getOperation(), PARTNER)).containsOnly(READ);
        assertThat(semanticRoleRepository.getAllRolesForPartner(WRITE.getOperation(), PARTNER)).containsOnly(WRITE);
        assertThat(semanticRoleRepository.getAllRolesForPartner(READ.getOperation(), OTHER_PARTNER)).containsOnly(READ);
        assertThat(semanticRoleRepository.getAllRolesForPartner(WRITE.getOperation(), OTHER_PARTNER)).isEmpty();
        assertThat(semanticRoleRepository.getAllRolesForAllPartners(READ.getOperation())).containsOnly(READ);
        assertThat(semanticRoleRepository.getAllRolesForAllPartners(WRITE.getOperation())).isEmpty();
    }

    private static JeapAuthenticationToken getToken(List<String> userRoles, Map<String, List<String>> businessPartnerRoles) {
        JeapAuthenticationConverter converter = new JeapAuthenticationConverter(new DefaultAuthoritiesResolver());
        Jwt jwt = Jwt.withTokenValue("dummy_token_value")
                .header("dummy_header", "dummy_header_value")
                .claim(USER_ROLES_CLAIM, userRoles)
                .claim(BUSINESS_PARTNER_ROLES_CLAIM, businessPartnerRoles)
                .build();
        return (JeapAuthenticationToken) converter.convert(jwt);
    }
}
