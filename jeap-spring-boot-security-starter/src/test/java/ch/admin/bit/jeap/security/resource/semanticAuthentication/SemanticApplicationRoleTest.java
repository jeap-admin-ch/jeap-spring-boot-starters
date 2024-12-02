package ch.admin.bit.jeap.security.resource.semanticAuthentication;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SemanticApplicationRoleTest {
    static List<SemanticApplicationRole> allWildcardPossibilities() {
        List<SemanticApplicationRole> allPossibilities = new LinkedList<>();
        for (boolean resourceWc : new boolean[]{true, false}) {
            for (boolean tenantWc : new boolean[]{true, false}) {
                for (boolean operationWc : new boolean[]{true, false}) {
                    allPossibilities.add(new SemanticApplicationRole("system",
                            tenantWc ? null : "tenant",
                            resourceWc ? null : "resource",
                            operationWc ? null : "operation"));
                }
            }
        }
        return allPossibilities;
    }

    @Test
    void builderEnsureNoWildcards() {
        //This must throw as no operation is given
        assertThrows(NullPointerException.class, () -> SemanticApplicationRole.builder()
                .system("system")
                .resource("resource")
                .tenant("tenant")
                .build());

        //This must throw as no resource is given
        assertThrows(NullPointerException.class, () -> SemanticApplicationRole.builder()
                .system("system")
                .tenant("tenant")
                .operation("operation")
                .build());

        //This must throw as no system is given
        assertThrows(NullPointerException.class, () -> SemanticApplicationRole.builder()
                .resource("resource")
                .tenant("tenant")
                .operation("operation")
                .build());

        //This is OK as tenant is optional
        SemanticApplicationRole.builder()
                .system("system")
                .resource("resource")
                .operation("operation")
                .build();

        //This is OK as well
        SemanticApplicationRole.builder()
                .system("system")
                .resource("resource")
                .tenant("tenant")
                .operation("operation")
                .build();
    }

    @Test
    void builderCreatedObject() {
        SemanticApplicationRole target = SemanticApplicationRole.builder()
                .system("system")
                .resource("resource")
                .operation("operation")
                .tenant("tenant")
                .build();

        assertEquals("system", target.getSystem());
        assertEquals("resource", target.getResource());
        assertEquals("operation", target.getOperation());
        assertEquals("tenant", target.getTenant());
    }

    @Test
    void builderCreatedObjectNoTenant() {
        SemanticApplicationRole target = SemanticApplicationRole.builder()
                .system("system")
                .resource("resource")
                .operation("operation")
                .build();

        assertEquals("system", target.getSystem());
        assertEquals("resource", target.getResource());
        assertEquals("operation", target.getOperation());
        Assertions.assertNull(target.getTenant());
    }

    @Test
    void fromTokenRole_invalid() {
        Optional<SemanticApplicationRole> target = SemanticApplicationRole.fromTokenRole("justAnOldRole_something");
        assertFalse(target.isPresent());
    }

    @Test
    void fromTokenRole_onlySystem() {
        SemanticApplicationRole target = SemanticApplicationRole.fromTokenRole("input").orElseThrow();

        assertEquals("input", target.getSystem());
        Assertions.assertNull(target.getTenant());
        Assertions.assertNull(target.getResource());
        Assertions.assertNull(target.getOperation());
    }

    @Test
    void fromTokenRole_onlySystemAndOperation() {
        SemanticApplicationRole target = SemanticApplicationRole.fromTokenRole("input_#read").orElseThrow();

        assertEquals("input", target.getSystem());
        Assertions.assertNull(target.getTenant());
        Assertions.assertNull(target.getResource());
        assertEquals("read", target.getOperation());
    }

    @Test
    void fromTokenRole_allButTenant() {
        SemanticApplicationRole target = SemanticApplicationRole.fromTokenRole("docbox_@decree_#read").orElseThrow();

        assertEquals("docbox", target.getSystem());
        Assertions.assertNull(target.getTenant());
        assertEquals("decree", target.getResource());
        assertEquals("read", target.getOperation());
    }

    @Test
    void fromTokenRole_all() {
        SemanticApplicationRole target = SemanticApplicationRole.fromTokenRole("input_%camiuns_@registrationcertificate_#update").orElseThrow();

        assertEquals("input", target.getSystem());
        assertEquals("camiuns", target.getTenant());
        assertEquals("registrationcertificate", target.getResource());
        assertEquals("update", target.getOperation());
    }

    @Test
    void fromTokenRole_wrongOrder() {
        Optional<SemanticApplicationRole> target = SemanticApplicationRole.fromTokenRole("input_@registrationcertificate_%camiuns_#update");

        assertTrue(target.isEmpty());
    }

    @Test
    void fromTokenRole_garbageAtEnd() {
        Optional<SemanticApplicationRole> target = SemanticApplicationRole.fromTokenRole("docbox_@decree_#read_garbage");

        assertTrue(target.isEmpty());
    }

    @Test
    void includes_equals() {
        SemanticApplicationRole target = new SemanticApplicationRole("system", "tenant", "resource", "operation");
        SemanticApplicationRole test = new SemanticApplicationRole("system", "tenant", "resource", "operation");

        assertTrue(target.includes(test));
    }

    @Test
    void matches_equals() {
        SemanticApplicationRole target = new SemanticApplicationRole("system", "tenant", "resource", "operation");
        SemanticApplicationRole test = new SemanticApplicationRole("system", "tenant", "resource", "operation");

        assertTrue(target.matches(test));
    }

    @Test
    void includes_different() {
        SemanticApplicationRole target = new SemanticApplicationRole("system", "tenant", "resource", "operation");
        SemanticApplicationRole test = new SemanticApplicationRole("system", "tenant2", "resource", "operation");

        assertFalse(target.includes(test));
    }

    @Test
    void matches_different() {
        SemanticApplicationRole target = new SemanticApplicationRole("system", "tenant", "resource", "operation");
        SemanticApplicationRole test = new SemanticApplicationRole("system", "tenant2", "resource", "operation");

        assertFalse(target.matches(test));
    }

    @ParameterizedTest
    @MethodSource("allWildcardPossibilities")
    void includes_wildcard(SemanticApplicationRole roleWithWildcard) {
        SemanticApplicationRole test = new SemanticApplicationRole("system", "tenant", "resource", "operation");

        assertTrue(roleWithWildcard.includes(test));
    }

    @ParameterizedTest
    @MethodSource("allWildcardPossibilities")
    void matches_wildcard(SemanticApplicationRole roleWithWildcard) {
        SemanticApplicationRole test = new SemanticApplicationRole("system", "tenant", "resource", "operation");

        assertTrue(roleWithWildcard.matches(test));
    }

    @Test
    void toStringAsInToken() {
        //All Given
        SemanticApplicationRole test = new SemanticApplicationRole("system", "tenant", "resource", "operation");
        assertEquals("system_%tenant_@resource_#operation", test.toString());

        //Some Wildcards
        SemanticApplicationRole testWc = new SemanticApplicationRole("system", null, "resource", null);
        assertEquals("system_@resource", testWc.toString());

        //Only System
        SemanticApplicationRole onlySystem = new SemanticApplicationRole("system", null, null, null);
        assertEquals("system", onlySystem.toString());
    }

    @Test
    void testSameOrWildCardForNoWildCardsRole() {
        SemanticApplicationRole noWildcardsRole = SemanticApplicationRole.builder().
                system("system").
                tenant("tenant").
                resource("resource").
                operation("operation").
                build();

        assertTrue(noWildcardsRole.sameSystem("system"));
        assertTrue(noWildcardsRole.sameTenantOrWildcard("tenant"));
        assertTrue(noWildcardsRole.sameResourceOrWildcard("resource"));
        assertTrue(noWildcardsRole.sameOperationOrWildcard("operation"));

        assertFalse(noWildcardsRole.sameSystem("different system"));
        assertFalse(noWildcardsRole.sameTenantOrWildcard("different tenant"));
        assertFalse(noWildcardsRole.sameResourceOrWildcard("different resource"));
        assertFalse(noWildcardsRole.sameOperationOrWildcard("different operation"));

        assertFalse(noWildcardsRole.sameSystem(""));
        assertFalse(noWildcardsRole.sameTenantOrWildcard(""));
        assertFalse(noWildcardsRole.sameResourceOrWildcard(""));
        assertFalse(noWildcardsRole.sameOperationOrWildcard(""));

        assertFalse(noWildcardsRole.sameSystem(null));
        assertFalse(noWildcardsRole.sameTenantOrWildcard(null));
        assertFalse(noWildcardsRole.sameResourceOrWildcard(null));
        assertFalse(noWildcardsRole.sameOperationOrWildcard(null));
    }

    @Test
    void testSameOrWildCardForAllWildCardsRole() {
        SemanticApplicationRole allWildcardsRole = new SemanticApplicationRole("system", null, null, null);

        assertTrue(allWildcardsRole.sameSystem("system"));
        assertTrue(allWildcardsRole.sameTenantOrWildcard("some tenant"));
        assertTrue(allWildcardsRole.sameResourceOrWildcard("some resource"));
        assertTrue(allWildcardsRole.sameOperationOrWildcard("some operation"));

        assertFalse(allWildcardsRole.sameSystem("different system"));

        assertFalse(allWildcardsRole.sameSystem(""));
        assertTrue(allWildcardsRole.sameTenantOrWildcard(""));
        assertTrue(allWildcardsRole.sameResourceOrWildcard(""));
        assertTrue(allWildcardsRole.sameOperationOrWildcard(""));

        assertFalse(allWildcardsRole.sameSystem(null));
        assertTrue(allWildcardsRole.sameTenantOrWildcard(null));
        assertTrue(allWildcardsRole.sameResourceOrWildcard(null));
        assertTrue(allWildcardsRole.sameOperationOrWildcard(null));
    }
}
