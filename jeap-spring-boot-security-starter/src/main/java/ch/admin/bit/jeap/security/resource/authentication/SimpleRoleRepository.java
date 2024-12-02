package ch.admin.bit.jeap.security.resource.authentication;

import lombok.Getter;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * A collection of user roles and business partner roles and the logic to query those roles for authorization purposes.
 */
@Getter
public class SimpleRoleRepository {

    private final Set<String> userRoles;
    private final Map<String, Set<String>> businessPartnerRoles;

    public SimpleRoleRepository(Set<String> userRoles, Map<String, Set<String>> businessPartnerRoles) {
        this.userRoles = Set.copyOf(userRoles);
        this.businessPartnerRoles = businessPartnerRoles.entrySet().stream()
                .collect(toUnmodifiableMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
    }

    /**
     * Check that the given role is present either as a user role or as a business partner role for any business partner.
     *
     * @param role The role to check for.
     * @return <code>true</code> if the role is present either as a user role or as a business partner role for any
     * business partner, <code>false</code> otherwise.
     */
    public boolean hasRole(String role) {
        return hasRoleForAllPartners(role) || businessPartnerRoles.values().stream().flatMap(Collection::stream).anyMatch(role::equals);
    }

    /**
     * Check that the given role is present for the given business partner
     *
     * @param role The role to check for.
     * @param businessPartner The name of the business partner for which the role must be present.
     * @return <code>true</code> if the role is present for the business partner, <code>false</code> otherwise.
     */
    public boolean hasRoleForPartner(String role, String businessPartner) {
        return hasRoleForAllPartners(role ) ||
                businessPartnerRoles.getOrDefault(businessPartner, emptySet()).contains(role);
    }

    /**
     * Check that the given role is present independently of a business partner.
     *
     * @param role The role to check for.
     * @return <code>true</code> if the role is present independently of a business partner, <code>false</code> otherwise.
     */
    public boolean hasRoleForAllPartners(String role) {
        return userRoles.contains(role);
    }

    /**
     * Get the names of the business partners for which the given role is present.
     *
     * @param role The role to check for.
     * @return The names of the business partners for which the role is present.
     */
    public Set<String> getPartnersForRole(String role) {
        return businessPartnerRoles.entrySet().stream()
                .filter( entry ->  entry.getValue().contains(role))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

}
