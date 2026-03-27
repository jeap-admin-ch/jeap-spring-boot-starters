package ch.admin.bit.jeap.security.resource.semanticAuthentication;

import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole.StringRepresentationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SemanticApplicationRoleTest {

    // === Method sources ===

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

    static Stream<Arguments> validRoleStrings() {
        return Stream.of(
                // STANDARD representations
                Arguments.of("mysystem", "mysystem", null, null, null, StringRepresentationType.STANDARD),
                Arguments.of("mysystem_#myoperation", "mysystem", null, null, "myoperation", StringRepresentationType.STANDARD),
                Arguments.of("docbox_@myresource_#myoperation", "docbox", null, "myresource", "myoperation", StringRepresentationType.STANDARD),
                Arguments.of("mysystem_%mytenant_@myresource_#myoperation", "mysystem", "mytenant", "myresource", "myoperation", StringRepresentationType.STANDARD),
                // EIAM representations
                Arguments.of("mysystem_!myoperation", "mysystem", null, null, "myoperation", StringRepresentationType.EIAM),
                Arguments.of("mysystem_@myresource_!myoperation", "mysystem", null, "myresource", "myoperation", StringRepresentationType.EIAM),
                Arguments.of("mysystem_:mytenant_@myresource_!myoperation", "mysystem", "mytenant", "myresource", "myoperation", StringRepresentationType.EIAM)
        );
    }

    static Stream<Arguments> invalidRoleStrings() {
        return Stream.of(
                // Not a semantic role
                Arguments.of("some-role_something"),
                // Wrong order - STANDARD
                Arguments.of("mysystem_@myresource_%mytenant_#myoperation"),
                // Wrong order - EIAM
                Arguments.of("mysystem_@myresource_:mytenant_!myoperation"),
                // Garbage at end - STANDARD
                Arguments.of("mysystem_@myresource_#myoperation_garbage"),
                // Garbage at end - EIAM
                Arguments.of("mysystem_@myresource_!myoperation_garbage"),
                // Mixed separators: STANDARD tenant + EIAM operation
                Arguments.of("mysystem_%mytenant_@myresource_!myoperation"),
                // Mixed separators: EIAM tenant + STANDARD operation
                Arguments.of("mysystem_:mytenant_@myresource_#myoperation")
        );
    }

    static Stream<Arguments> toStringCases() {
        return Stream.of(
                // STANDARD - all parts
                Arguments.of(new SemanticApplicationRole("system", "tenant", "resource", "operation"),
                        "system_%tenant_@resource_#operation"),
                // STANDARD - some wildcards
                Arguments.of(new SemanticApplicationRole("system", null, "resource", null),
                        "system_@resource"),
                // STANDARD - only system
                Arguments.of(new SemanticApplicationRole("system", null, null, null),
                        "system"),
                // EIAM - all parts
                Arguments.of(new SemanticApplicationRole("system", "tenant", "resource", "operation", StringRepresentationType.EIAM),
                        "system_:tenant_@resource_!operation"),
                // EIAM - some wildcards
                Arguments.of(new SemanticApplicationRole("system", null, "resource", null, StringRepresentationType.EIAM),
                        "system_@resource"),
                // EIAM - only system
                Arguments.of(new SemanticApplicationRole("system", null, null, null, StringRepresentationType.EIAM),
                        "system")
        );
    }

    // === Builder tests ===

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
        assertNull(target.getTenant());
    }

    @Test
    void builderDefaultsToStandardRepresentation() {
        SemanticApplicationRole role = SemanticApplicationRole.builder()
                .system("system")
                .resource("resource")
                .operation("operation")
                .build();
        assertEquals(StringRepresentationType.STANDARD, role.getRepresentationType());
    }

    @Test
    void stringRepresentationType_from() {
        assertEquals(StringRepresentationType.STANDARD, StringRepresentationType.from("mysystem"));
        assertEquals(StringRepresentationType.STANDARD, StringRepresentationType.from("mysystem_@myresource"));
        assertEquals(StringRepresentationType.STANDARD, StringRepresentationType.from("mysystem_%mytenant_@myresource_#myoperation"));
        assertEquals(StringRepresentationType.EIAM, StringRepresentationType.from("mysystem_:mytenant_@myresource_!myoperation"));
        assertEquals(StringRepresentationType.EIAM, StringRepresentationType.from("mysystem_!myoperation"));
        assertEquals(StringRepresentationType.EIAM, StringRepresentationType.from("mysystem_:mytenant"));
        // Mixed separators return null
        assertNull(StringRepresentationType.from("mysystem_%mytenant_@myresource_!myoperation"));
        assertNull(StringRepresentationType.from("mysystem_:mytenant_@myresource_#myoperation"));
    }

    @ParameterizedTest
    @MethodSource("validRoleStrings")
    void fromTokenRole_validRoles(String tokenRole, String expectedSystem, String expectedTenant,
                                 String expectedResource, String expectedOperation,
                                 StringRepresentationType expectedType) {
        SemanticApplicationRole target = SemanticApplicationRole.fromTokenRole(tokenRole).orElseThrow();

        assertEquals(expectedSystem, target.getSystem());
        assertEquals(expectedTenant, target.getTenant());
        assertEquals(expectedResource, target.getResource());
        assertEquals(expectedOperation, target.getOperation());
        assertEquals(expectedType, target.getRepresentationType());
    }

    @ParameterizedTest
    @MethodSource("invalidRoleStrings")
    void fromTokenRole_invalidRoles(String tokenRole) {
        assertTrue(SemanticApplicationRole.fromTokenRole(tokenRole).isEmpty());
    }

    @Test
    void fromTokenRole_ambiguousStrings_defaultToTypeStandard() {
        // "mysystem" has no type-specific separators - auto-detects as STANDARD
        SemanticApplicationRole systemOnly = SemanticApplicationRole.fromTokenRole("mysystem").orElseThrow();
        assertEquals(StringRepresentationType.STANDARD, systemOnly.getRepresentationType());

        // "mysystem_@myresource" uses only the shared resource separator - auto-detects as STANDARD
        SemanticApplicationRole withResource = SemanticApplicationRole.fromTokenRole("mysystem_@myresource").orElseThrow();
        assertEquals(StringRepresentationType.STANDARD, withResource.getRepresentationType());
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

    @ParameterizedTest
    @MethodSource("toStringCases")
    void toString_sameAsInToken(SemanticApplicationRole role, String expected) {
        assertEquals(expected, role.toString());
    }

    @SuppressWarnings("unused")
    @ParameterizedTest
    @MethodSource("validRoleStrings")
    void roundtrip_parseAndToString(String tokenRole, String expectedSystem, String expectedTenant,
                                    String expectedResource, String expectedOperation,
                                    StringRepresentationType expectedType) {
        SemanticApplicationRole parsed = SemanticApplicationRole.fromTokenRole(tokenRole).orElseThrow();
        assertEquals(tokenRole, parsed.toString());
    }

    @Test
    void equalityAcrossRepresentations() {
        SemanticApplicationRole standard = SemanticApplicationRole.fromTokenRole(
                "mysystem_%mytenant_@myresource_#myoperation").orElseThrow();
        SemanticApplicationRole eiam = SemanticApplicationRole.fromTokenRole(
                "mysystem_:mytenant_@myresource_!myoperation").orElseThrow();

        assertEquals(standard, eiam);
        assertEquals(standard.hashCode(), eiam.hashCode());
    }

    @Test
    void matchingAcrossRepresentations() {
        SemanticApplicationRole standard = SemanticApplicationRole.fromTokenRole(
                "mysystem_%mytenant_@myresource_#myoperation").orElseThrow();
        SemanticApplicationRole eiam = SemanticApplicationRole.fromTokenRole(
                "mysystem_:mytenant_@myresource_!myoperation").orElseThrow();

        assertTrue(standard.includes(eiam));
        assertTrue(eiam.includes(standard));
        assertTrue(standard.matches(eiam));
        assertTrue(eiam.matches(standard));
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

    @Test
    void containsAnySeparatorCharacter_withSeparatorChars_returnsTrue() {
        assertTrue(StringRepresentationType.containsAnySeparatorCharacter("resource@operation"));
        assertTrue(StringRepresentationType.containsAnySeparatorCharacter("tenant%something"));
        assertTrue(StringRepresentationType.containsAnySeparatorCharacter("something#operation"));
        assertTrue(StringRepresentationType.containsAnySeparatorCharacter("tenant:something"));
        assertTrue(StringRepresentationType.containsAnySeparatorCharacter("something!operation"));
        assertTrue(StringRepresentationType.containsAnySeparatorCharacter("system_%tenant_@resource_#operation"));
        assertTrue(StringRepresentationType.containsAnySeparatorCharacter("system_:tenant_@resource_!operation"));
    }

    @Test
    void containsAnySeparatorCharacter_withValidParams_returnsFalse() {
        assertFalse(StringRepresentationType.containsAnySeparatorCharacter("read"));
        assertFalse(StringRepresentationType.containsAnySeparatorCharacter("auth"));
        assertFalse(StringRepresentationType.containsAnySeparatorCharacter("my-resource"));
        assertFalse(StringRepresentationType.containsAnySeparatorCharacter("my_resource"));
        assertFalse(StringRepresentationType.containsAnySeparatorCharacter(""));
        assertFalse(StringRepresentationType.containsAnySeparatorCharacter(null));
    }
}
