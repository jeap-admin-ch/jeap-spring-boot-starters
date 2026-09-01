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
     * Name of this resource, checked against the audience ('aud' claim) of access tokens.
     * Defaults to the application name if not set explicitly.
     */
    private String resourceId;

    /**
     * Name of the application, used as default for the resource id if it was not set explicitly.
     */
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Name of the system to check against in semantic roles. Setting this activates semantic role authorization.
     */
    private String systemName;

    /**
     * Controls how access tokens in the USER and SYS contexts that do not specify an audience ('aud' claim missing or
     * empty) are treated: OFF (default) accepts them as valid for every resource (legacy behaviour), ON rejects them,
     * and WARN accepts them but logs a warning for each token that would be rejected in mode ON (migration aid).
     * This setting does not impact tokens in the B2B context which remain not audience-checked.
     */
    private StrictAudienceValidationMode strictAudienceValidation = StrictAudienceValidationMode.OFF;

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
    List<@Valid AuthorizationServerConfigProperties> authServers;

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
    public void initialize() {
        applyDefaults();
        validate();
    }

    /**
     * Complete the configuration with derived defaults: the resource id defaults to the application name, and if
     * introspection is active, the introspection configurations of the auth servers get their introspection uri
     * derived from the issuer and the resource id set as default client id.
     */
    public void applyDefaults() {
        if (!StringUtils.hasText(resourceId)) {
            resourceId = applicationName;
        }
        if (isIntrospectionActive()) {
            getAllAuthServerConfigurations().stream()
                    .filter(config -> config.getIntrospection() != null)
                    .forEach(config -> config.getIntrospection().applyDefaults(config.getIssuer(), resourceId));
        }
    }

    private IntrospectionMode getIntrospectionMode() {
        return introspection != null ? introspection.getMode() : null;
    }

    private boolean isIntrospectionActive() {
        IntrospectionMode introspectionMode = getIntrospectionMode();
        return introspectionMode != null && introspectionMode.doesActivateIntrospection();
    }

    @SuppressWarnings("java:S3776")
    public void validate() {
        log.info("Validating resource server properties for resource id {}", resourceId);
        IntrospectionMode introspectionMode = getIntrospectionMode();
        if (introspectionMode == null) {
            for (AuthorizationServerConfigProperties config : getAllAuthServerConfigurations()) {
                if (config.getIntrospection() != null) {
                    throw new IllegalArgumentException(config.getIssuer() + ": introspection has not been activated but introspection configurations have been provided. Did you forget to activate introspection by setting an introspection mode?");
                }
            }
        } else if (introspectionMode.doesActivateIntrospection()) {
            for (AuthorizationServerConfigProperties config : getAllAuthServerConfigurations()) {
                if (config.getIntrospection() == null) {
                    throw new IllegalArgumentException(config.getIssuer() + ": introspection configuration must be defined when introspection mode is activated.");
                }
                config.getIntrospection().validate(config.getIssuer());
            }
        } else {
            for (AuthorizationServerConfigProperties config : getAllAuthServerConfigurations()) {
                if (config.getIntrospection() != null) {
                    log.warn("{}: introspection disabled with introspection mode \"{}\", but introspection configurations provided.", config.getIssuer(), introspectionMode);
                }
            }
        }
    }

}
