package ch.admin.bit.jeap.swagger;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

/**
 * Configurations for Swagger in Blueprint Microservice
 */
@AutoConfiguration
@PropertySource("classpath:SwaggerDefaultConfiguration.properties")
@ConfigurationProperties(prefix = "jeap.swagger")
@Data
@NoArgsConstructor
public class SwaggerProperties {
    @NestedConfigurationProperty
    private Oauth oauth;
    @NestedConfigurationProperty
    private Secured secured;

    /**
     * The status of swagger in the project:
     * * {@link SwaggerStatus#OPEN} swagger can be accessed without authentication,
     * * {@link SwaggerStatus#SECURED} swagger can only be accessed with basic auth,
     * * {@link SwaggerStatus#DISABLED} swagger cannot be accessed
     * * {@link SwaggerStatus#CUSTOM} no spring security configuration at all
     * Default is DISABLED, but you can set it to OPEN for local environments
     */
    @NonNull
    private SwaggerStatus status;
    /**
     * Ant-Patters which paths are part of swagger ui. Usually you do not need to change this
     */
    @NonNull
    private List<String> antPathPatters;

    /**
     * When set to true, Server definitions delivered to Swagger Frontend will have https as protocol instead of http.
     * This is useful for architectures where the microservice listens to http and is behind a load balancer or reverse
     * proxy configured to listen to https
     */
    private boolean enforceServerBaseHttps;

    public enum SwaggerStatus {OPEN, SECURED, DISABLED, CUSTOM}

    @Data
    private static class Oauth {
        /**
         * The URL of the discovery endpoint of the authorization server
         */
        private String openIdConnectUrl;
    }

    @Data
    static class Secured {
        /**
         * The username for basic authentication when securing swagger
         */
        private String username;
        /**
         * The password for basic authentication when securing swagger
         */
        private String password;
    }
}
