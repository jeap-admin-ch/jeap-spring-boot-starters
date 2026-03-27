package ch.admin.bit.jeap.security.resource.semanticAuthentication;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * A single semantic role split in its parts
 */
@Slf4j
@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class SemanticApplicationRole {
    String system;
    String tenant;
    String resource;
    String operation;
    @EqualsAndHashCode.Exclude
    StringRepresentationType representationType;

    /**
     * The supported string representation types for semantic roles.
     */
    enum StringRepresentationType {

        /** Standard representation using system_%tenant_@resource_#operation */
        STANDARD('%', '#'),

        /** Representation using system_:tenant_@resource_!operation
         * (added because eIAM doesn't support the characters '%' and '#' in its role names)
         * */
        EIAM(':', '!');

        static final char RESOURCE_SEPARATOR = '@';

        private final char tenantSeparator;
        private final char operationSeparator;

        StringRepresentationType(char tenantSeparator, char operationSeparator) {
            this.tenantSeparator = tenantSeparator;
            this.operationSeparator = operationSeparator;
        }

        /**
         * Detects the string representation type of a token role string.
         *
         * @param tokenRole The token role string to detect the representation type for
         * @return The detected representation type, or null if the string contains mixed separators
         */
        public static StringRepresentationType from(String tokenRole) {
            boolean hasSpecificStandardSeparators = STANDARD.hasSpecificSeparators(tokenRole);
            boolean hasSpecificEiamSeparators = EIAM.hasSpecificSeparators(tokenRole);

            if (hasSpecificStandardSeparators && hasSpecificEiamSeparators) {
                log.error("Token role '{}' contains separators from both STANDARD and EIAM representations.", tokenRole);
                return null;
            }

            // no specific eIAM separators -> only standard or common separators -> standard type
            return hasSpecificEiamSeparators ? EIAM: STANDARD;
        }

        /**
         * Checks if the given value contains any of the separator characters used in semantic role strings.
         * This can be used to detect misuse where a full token role string is passed as a single parameter
         * instead of using decomposed parameters.
         *
         * @param value The value to check
         * @return true if the value contains any separator character
         */
        static boolean containsAnySeparatorCharacter(String value) {
            if (value == null) {
                return false;
            }
            if (value.indexOf(RESOURCE_SEPARATOR) >= 0) {
                return true;
            }
            for (StringRepresentationType type : values()) {
                if (value.indexOf(type.tenantSeparator) >= 0 || value.indexOf(type.operationSeparator) >= 0) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasSpecificSeparators(String tokenRole) {
            return tokenRole.contains("_" + tenantSeparator) || tokenRole.contains("_" + operationSeparator);
        }
    }

    SemanticApplicationRole(String system, String tenant, String resource, String operation) {
        this(system, tenant, resource, operation, StringRepresentationType.STANDARD);
    }

    /**
     * Public builder for semantic roles. No wildcards are allowed here except tenant
     *
     * @param system    The system
     * @param resource  The resource
     * @param tenant    The tenant (can be null if not needed)
     * @param operation The operation
     * @return The semantic role
     */
    @SuppressWarnings("unused")
    @Builder
    private static SemanticApplicationRole create(@NonNull String system, String tenant, @NonNull String resource, @NonNull String operation) {
        return new SemanticApplicationRole(system, tenant, resource, operation);
    }

    /**
     * Internal factory method to parse tokens in {@link SemanticRoleRepository}.
     * Auto-detects the string representation type.
     */
    static Optional<SemanticApplicationRole> fromTokenRole(String tokenRole) {
        StringRepresentationType type = StringRepresentationType.from(tokenRole);
        if (type == null) {
            return Optional.empty();
        }
        return parseTokenRole(tokenRole, type);
    }

    private static Optional<SemanticApplicationRole> parseTokenRole(String tokenRole, StringRepresentationType type) {
        String[] splitsArray = tokenRole.split("_");
        int index = 0;

        //The system must be the first part
        String system = splitsArray[index++];

        //Next could be the tenant
        String tenant = fetchElementStartsWith(splitsArray, index, type.tenantSeparator);
        if (tenant != null) {
            index++;
        }

        //Next could be the resource
        String resource = fetchElementStartsWith(splitsArray, index, StringRepresentationType.RESOURCE_SEPARATOR);
        if (resource != null) {
            index++;
        }

        //Next could be the operation
        String operation = fetchElementStartsWith(splitsArray, index, type.operationSeparator);
        if (operation != null) {
            index++;
        }

        //If there is something left, this is not a semantic role
        if (splitsArray.length > index) {
            return Optional.empty();
        }

        SemanticApplicationRole role = new SemanticApplicationRole(system, tenant, resource, operation, type);
        return Optional.of(role);
    }

    private static String fetchElementStartsWith(String[] splits, int index, char startWith) {
        if (splits.length <= index) {
            return null;
        }
        if (splits[index].charAt(0) == startWith) {
            return splits[index].substring(1);
        }
        return null;
    }

    /**
     * Check if this role includes another role. This is the case if the systems are the same and the tenants/resources/operations
     * are the same or are a wildcard (null) in this role.
     *
     * @param roleToCheckAgainst other role the check
     * @return True if this role includes roleToCheckAgainst
     */
    boolean includes(SemanticApplicationRole roleToCheckAgainst) {
        return sameSystem(roleToCheckAgainst.getSystem()) &&
                sameTenantOrWildcard(roleToCheckAgainst.getTenant()) &&
                sameResourceOrWildcard(roleToCheckAgainst.getResource()) &&
                sameOperationOrWildcard(roleToCheckAgainst.getOperation());
    }


    /**
     * Check if this role matches another role. This is the case if the systems are the same and the tenants/resources/operations
     * are the same or are a wildcard (null) in this role or the role to match.
     *
     * @param roleToMatch role to match
     * @return True if this role matches the given role.
     */
    boolean matches(SemanticApplicationRole roleToMatch) {
        return  roleToMatch.sameSystem(system) &&
                (tenant == null || roleToMatch.sameTenantOrWildcard(tenant)) &&
                (resource == null || roleToMatch.sameResourceOrWildcard(resource)) &&
                (operation == null || roleToMatch.sameOperationOrWildcard(operation));
    }


    /**
     * Checks if the role is for the given system
     *
     * @param system A system name
     * @return True if the role's system equals the given system name
     */
    boolean sameSystem(String system) {
        return this.system.equals(system);
    }

    /**
     * Checks if the role's tenant is the same as the given tenant or a wildcard
     *
     * @param tenant A tenant name or null for wildcard
     * @return True if the role's tenant equals the given tenant name or is a wildcard
     */
    boolean sameTenantOrWildcard(String tenant) {
        return this.tenant == null || this.tenant.equals(tenant);
    }

    /**
     * Checks if the role's resource is the same as the given resource or a wildcard
     *
     * @param resource A resource name
     * @return True if the role's resource equals the given resource name or is a wildcard
     */
    boolean sameResourceOrWildcard(String resource) {
        return this.resource == null || this.resource.equals(resource);
    }

    /**
     * Checks if the role's operation is the same as the given operation or a wildcard
     *
     * @param operation A operation name
     * @return True if the role's operation equals the given operation name or is a wildcard
     */
    boolean sameOperationOrWildcard(String operation) {
        return this.operation == null || this.operation.equals(operation);
    }

    /**
     * @return The role as written in the token
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(system);

        if (tenant != null) {
            sb.append("_");
            sb.append(representationType.tenantSeparator);
            sb.append(tenant);
        }

        if (resource != null) {
            sb.append("_");
            sb.append(StringRepresentationType.RESOURCE_SEPARATOR);
            sb.append(resource);
        }

        if (operation != null) {
            sb.append("_");
            sb.append(representationType.operationSeparator);
            sb.append(operation);
        }
        return sb.toString();
    }
}
