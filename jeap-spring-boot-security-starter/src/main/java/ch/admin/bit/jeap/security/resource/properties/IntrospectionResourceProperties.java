package ch.admin.bit.jeap.security.resource.properties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Introspection configuration on the resource server level.
 */
@Data
@Slf4j
public class IntrospectionResourceProperties {

    /**
    * The introspection mode. May be left unconfigured if introspection is not needed.
    */
    private IntrospectionMode mode;

}
