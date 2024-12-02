package ch.admin.bit.jeap.security.resource.semanticAuthentication;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import lombok.Value;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A collection of semantic roles applying to a given system extracted from a given jeap authentication token.
 * Implements the logic to query for roles e.g. for autorisation purposes.Only considers roles applying to the configured
 * system, i.e. if an authentication token also includes roles for different systems, those roles will be ignored.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@Value
public class SemanticRoleRepository {
    Map<String, Set<SemanticApplicationRole>> businessPartnerRoles;
    Set<SemanticApplicationRole> userRoles;
    String systemName;

    public SemanticRoleRepository(String systemName, JeapAuthenticationToken jeapAuthenticationToken) {
        this.userRoles = jeapAuthenticationToken.getUserRoles().stream()
                .map(SemanticApplicationRole::fromTokenRole)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(r -> r.sameSystem(systemName))
                .collect(Collectors.toSet());
        Map<String, Set<SemanticApplicationRole>> businessPartnerRoles = new HashMap<>();
        jeapAuthenticationToken.getBusinessPartnerRoles().forEach((partner, roleList) ->
                businessPartnerRoles.put(partner, roleList.stream()
                        .map(SemanticApplicationRole::fromTokenRole)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .filter(r -> r.sameSystem(systemName))
                        .collect(Collectors.toSet())
                )
        );
        this.businessPartnerRoles = businessPartnerRoles;
        this.systemName = systemName;
    }

    /**
     * Check that the current user has a given role for a given business partner
     *
     * @param role    For role to check for
     * @param partner The name of the business partner for whom the role must be given
     * @return true if the user can perform this action
     */
    public boolean hasRoleForPartner(SemanticApplicationRole role, String partner) {
        if (hasRoleForAllPartners(role)) {
            return true;
        }
        Set<SemanticApplicationRole> partnerRoles = businessPartnerRoles.get(partner);
        if (partnerRoles != null) {
            return partnerRoles.stream()
                    .anyMatch(role::matches);
        }
        return false;
    }

    /**
     * Check that the current user has a given role for a given business partner
     *
     * @param operation The operation to check for
     * @param partner  The name of the business partner for whom the role must be given
     * @return true if the user can perform this action
     */
    public boolean hasRoleForPartner(String operation, String partner) {
        SemanticApplicationRole role = new SemanticApplicationRole(systemName, null, null, operation);
        return hasRoleForPartner(role, partner);
    }

    /**
     * Check that the current user has a given role for a given business partner
     *
     * @param resource The resource to check for
     * @param operation The operation to check for
     * @param partner  The name of the business partner for whom the role must be given
     * @return true if the user can perform this action
     */
    public boolean hasRoleForPartner(String resource, String operation, String partner) {
        SemanticApplicationRole role = SemanticApplicationRole.builder()
                .system(systemName)
                .resource(resource)
                .operation(operation)
                .build();
        return hasRoleForPartner(role, partner);
    }

    /**
     * Check that the current user has a given role for a given business partner
     *
     * @param tenant   The tenant to check for
     * @param resource The resource to check for
     * @param operation The operation to check for
     * @param partner  The name of the business partner for whom the role must be given
     * @return true if the user can perform this action
     */
    public boolean hasRoleForPartner(String tenant, String resource, String operation, String partner) {
        SemanticApplicationRole role = SemanticApplicationRole.builder()
                .system(systemName)
                .tenant(tenant)
                .resource(resource)
                .operation(operation)
                .build();
        return hasRoleForPartner(role, partner);
    }

    /**
     * Check that the current user has a given role for at least one business partner. This can  e.g. be used for coarse grained
     * authentication when you do not know yet to which partner a resource belongs.
     *
     * @param role For role to check for
     * @return true if the user can perform this action
     */
    public boolean hasRole(SemanticApplicationRole role) {
        if (hasRoleForAllPartners(role)) {
            return true;
        }
        return businessPartnerRoles.values().stream()
                .flatMap(Collection::stream)
                .anyMatch(role::matches);
    }


    /**
     * Check that the current user has a given role for at least one business partner. This can  e.g. be used for coarse grained
     * authentication when you do not know yet to which partner a resource belongs.
     *
     * @param operation The operation to check for
     * @return true if the user can perform this action
     */
    public boolean hasOperation(String operation) {
        SemanticApplicationRole role = new SemanticApplicationRole(systemName, null, null, operation);
        return hasRole(role);
    }

    /**
     * Check that the current user has a given role for at least one business partner. This can  e.g. be used for coarse grained
     * authentication when you do not know yet to which partner a resource belongs.
     *
     * @param resource The resource to check for
     * @param operation The operation to check for
     * @return true if the user can perform this action
     */
    public boolean hasRole(String resource, String operation) {
        SemanticApplicationRole role = SemanticApplicationRole.builder()
                .system(systemName)
                .resource(resource)
                .operation(operation)
                .build();
        return hasRole(role);
    }

    /**
     * Check that the current user has a given role for at least one business partner. This can  e.g. be used for coarse grained
     * authentication when you do not know yet to which partner a resource belongs.
     *
     * @param tenant   The tenant to check for
     * @param resource The resource to check for
     * @param operation The operation to check for
     * @return true if the user can perform this action
     */
    public boolean hasRole(String tenant, String resource, String operation) {
        SemanticApplicationRole role = SemanticApplicationRole.builder()
                .system(systemName)
                .tenant(tenant)
                .resource(resource)
                .operation(operation)
                .build();
        return hasRole(role);
    }

    /**
     * Check that the current user has a given role for ALL business partners. This return true only if a user
     * is allowed to use this role for ALL business partners, e.g. if he or she is an internal user
     *
     * @param role For role to check for
     * @return true if the user can perform this action
     */
    public boolean hasRoleForAllPartners(SemanticApplicationRole role) {
        return userRoles.stream().anyMatch(role::matches);
    }


    /**
     * Check that the current user has a given role for ALL business partners. This return true only if a user
     * is allowed to use this role for ALL business partners, e.g. if he or she is an internal user
     *
     * @param operation The operation to check for
     * @return true if the user can perform this action
     */
    public boolean hasRoleForAllPartners(String operation) {
        SemanticApplicationRole role = new SemanticApplicationRole(systemName, null, null, operation);
        return hasRoleForAllPartners(role);
    }

    /**
     * Check that the current user has a given role for ALL business partners. This return true only if a user
     * is allowed to use this role for ALL business partners, e.g. if he or she is an internal user
     *
     * @param resource  The resource to check for
     * @param operation The operation to check for
     * @return true if the user can perform this action
     */
    public boolean hasRoleForAllPartners(String resource, String operation) {
        SemanticApplicationRole role = SemanticApplicationRole.builder()
                .system(systemName)
                .resource(resource)
                .operation(operation)
                .build();
        return hasRoleForAllPartners(role);
    }

    /**
     * Check that the current user has a given role for ALL business partners. This return true only if a user
     * is allowed to use this role for ALL business partners, e.g. if he or she is an internal user
     *
     * @param tenant   The tenant to check for
     * @param resource The resource to check for
     * @param operation The operation to check for
     * @return true if the user can perform this action
     */
    public boolean hasRoleForAllPartners(String tenant, String resource, String operation) {
        SemanticApplicationRole role = SemanticApplicationRole.builder()
                .system(systemName)
                .tenant(tenant)
                .resource(resource)
                .operation(operation)
                .build();
        return hasRoleForAllPartners(role);
    }

    /**
     * Get all roles the current user has for a given operation and business partner. This includes the roles a user has
     * for all partners i.e. users roles for the given operation.
     *
     * @param operation The operation for which to get all the roles
     * @param partner  The name of the business partner for whom the role must be given
     * @return A list of the roles. Those roles can have certain fields set to null if they are wildcards.
     */
    public Collection<SemanticApplicationRole> getAllRolesForPartner(String operation, String partner) {
        List<SemanticApplicationRole> result = new LinkedList<>();
        userRoles.stream()
                .filter(role -> role.sameOperationOrWildcard(operation))
                .forEach(result::add);
        Set<SemanticApplicationRole> partnerRoles = businessPartnerRoles.get(partner);
        if (partnerRoles != null) {
            partnerRoles.stream()
                    .filter(role -> role.sameOperationOrWildcard(operation))
                    .forEach(result::add);
        }
        return result;
    }

    /**
     * Get all roles the current user has for a given operation
     *
     * @param operation The operation for which to get all the roles
     * @return A list of the roles. Those roles can have certain fields set to null if they are wildcards.
     */
    public Collection<SemanticApplicationRole> getAllRoles(String operation) {
        List<SemanticApplicationRole> result = new LinkedList<>();

        userRoles.stream()
                .filter(role -> role.sameOperationOrWildcard(operation))
                .forEach(result::add);
        businessPartnerRoles.values().stream()
                .flatMap(Collection::stream)
                .filter(role -> role.sameOperationOrWildcard(operation))
                .forEach(result::add);
        return result;
    }

    /**
     * Get all roles the current user has for a given operation and ALL business partners
     *
     * @param operation The operation for which to get all the roles
     * @return A list of the roles. Those roles can have certain fields set to null if they are wildcards.
     */
    public Collection<SemanticApplicationRole> getAllRolesForAllPartners(String operation) {
        List<SemanticApplicationRole> result = new LinkedList<>();
        userRoles.stream()
                .filter(role -> role.sameOperationOrWildcard(operation))
                .forEach(result::add);
        return result;
    }

    /**
     * Get all partners for which the current user has at least one role that matches the given role.
     *
     * @param role The role to check for.
     * @return The partners
     */
    public Collection<String> getPartnersForRole(SemanticApplicationRole role) {
        return getPartnersForRole(role::matches);
    }

    /**
     * Get all partners for which the current user has at least one role for the given resource with the given operation.
     *
     * @param operation The operation to check for.
     * @return The partners
     */
    public Collection<String> getPartnersForRole(String operation) {
        return getPartnersForRole(new SemanticApplicationRole(systemName, null, null, operation));
    }

    /**
     * Get all partners for which the current user has at least one role for the given resource with the given operation.
     *
     * @param resource The resource to check for.
     * @param operation The operation to check for.
     * @return The partners
     */
    public Collection<String> getPartnersForRole(String resource, String operation) {
        return getPartnersForRole(SemanticApplicationRole.builder().
                system(systemName).resource(resource).operation(operation).build());
    }

    /**
     * Get all partners for which the current user has at least one role for the given tenant and resource with the given operation.
     *
     * @param tenant The tenant to check for.
     * @param resource The resource to check for.
     * @param operation The operation to check for.
     * @return The partners
     */
    public Collection<String> getPartnersForRole(String tenant, String resource, String operation) {
        return getPartnersForRole(SemanticApplicationRole.builder().
                system(systemName).tenant(tenant).resource(resource).operation(operation).build());    }

    /**
     * Get all partners for which the current user has at least one role that matches the given role predicate.
     *
     * @param rolePredicate The tenant to check for.
     * @return The partners
     */
    public Collection<String> getPartnersForRole(Predicate<SemanticApplicationRole> rolePredicate) {
        return businessPartnerRoles.entrySet().stream()
                .filter( entry ->  entry.getValue().stream()
                        .anyMatch(rolePredicate))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

}
