package ch.admin.bit.jeap.security.resource.properties;

import ch.admin.bit.jeap.security.resource.configuration.JeapOAuth2ResourceCondition;
import lombok.Data;
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
public class ResourceServerProperties {

    /**
     * Name of the resource, used for tokens with restricted audience
     */
    private String resourceId;

    /**
     * Name of the application, used if  no resource was defined
     */
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Name of the system to check against in semantic roles. Setting this activates semantic role authorization.
     */
    private String systemName;

    /**
     * Auth server configuration for the user and system authentication context.
     * Shortcut configuration option, left intact for backward compatibility.
     */
    @NestedConfigurationProperty
    private AuthorizationServerConfigProperties authorizationServer;

    /**
     * Auth server configuration for the business-to-business authentication context.
     * Shortcut configuration option, left intact for backward compatibility.
     */
    @NestedConfigurationProperty
    private B2BGatewayConfigProperties b2BGateway;

    /**
     * Configurations of the auth servers to be trusted by this resource server.
     */
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

}
