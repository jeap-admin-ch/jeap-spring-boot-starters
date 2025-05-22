package ch.admin.bit.jeap.security.resource.properties;

import ch.admin.bit.jeap.security.resource.configuration.JeapOAuth2ResourceCondition;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;


/**
 * Configuration properties to configure the OAuth2 resource server(s).
 */
@AutoConfiguration
@Conditional(JeapOAuth2ResourceCondition.class)
@ConfigurationProperties("jeap.security.oauth2.resourceserver")
@Validated
@Data
@Slf4j
public class ResourceServerProperties {

    /**
     * Name of the resource, used for tokens with restricted audience
     */
    private String resourceId;

    /**
     * Name of the application, used if no resource was defined
     */
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Name of the system to check against in semantic roles. Setting this activates semantic role authorization.
     */
    private String systemName;

    /**
     * Introspection configuration on the resource level.
     */
    @NestedConfigurationProperty
    @Valid
    private IntrospectionResourceProperties introspection;

    /**
     * Auth server configuration for the user and system authentication context.
     * Shortcut configuration option, left intact for backward compatibility.
     */
    @NestedConfigurationProperty
    @Valid
    private AuthorizationServerConfigProperties authorizationServer;

    /**
     * Auth server configuration for the business-to-business authentication context.
     * Shortcut configuration option, left intact for backward compatibility.
     */
    @NestedConfigurationProperty
    @Valid
    private B2BGatewayConfigProperties b2BGateway;

    /**
     * Configurations of the auth servers to be trusted by this resource server.
     */
    @Valid
    List<AuthorizationServerConfigProperties> authServers;

    public String getAudience() {
        return StringUtils.hasText(resourceId) ? resourceId : applicationName;
    }

    /**
     * Get all auth server configurations configured by these configuration properties.
     *
     * @return All auth server configurations.
     */
    public List<AuthorizationServerConfigProperties> getAllAuthServerConfigurations() {
        List<AuthorizationServerConfigProperties> allAuthServerConfigs = new ArrayList<>();
        if (authorizationServer != null) {
            allAuthServerConfigs.add(authorizationServer);
        }
        if (b2BGateway != null) {
            allAuthServerConfigs.add(b2BGateway.asAuthorizationServerConfigProperties());
        }
        if (authServers != null) {
            allAuthServerConfigs.addAll(authServers);
        }
        return allAuthServerConfigs;
    }

    @PostConstruct
    @SuppressWarnings("java:S3776")
    public void validate() {
        log.info("Validating resource server properties for resource id {}", resourceId);
        IntrospectionMode introspectionMode = introspection != null ? introspection.getMode() : null;
        if (introspectionMode == null) {
            for (AuthorizationServerConfigProperties config : getAllAuthServerConfigurations()) {
                if (config.getIntrospection() != null) {
                    throw new IllegalArgumentException(config.getIssuer() + ": introspection has not been activated but introspection configurations have been provided. Did you forget to activate introspection by setting an introspection mode?");
                }
            }

        } else if (IntrospectionMode.NONE.equals(introspectionMode)) {
            for (AuthorizationServerConfigProperties config : getAllAuthServerConfigurations()) {
                if (config.getIntrospection() != null) {
                    log.warn("{}: introspection disabled with introspection mode \"NONE\", but introspection configurations provided.", config.getIssuer());
                }
            }

        } else {
            for (AuthorizationServerConfigProperties config : getAllAuthServerConfigurations()) {
                if (config.getIntrospection() == null) {
                    throw new IllegalArgumentException(config.getIssuer() + ": introspection configuration must be defined when introspection mode is activated.");
                }
                config.getIntrospection().validate(config.getIssuer());
            }
        }

    }

}
