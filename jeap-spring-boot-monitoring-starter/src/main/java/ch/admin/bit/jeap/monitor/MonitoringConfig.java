package ch.admin.bit.jeap.monitor;

import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.PropertySource;

/**
 * Configuration for monitoring starter
 */
@AutoConfiguration
@ConfigurationProperties(prefix = "jeap.monitor")
@PropertySource(value = {"classpath:jeap-actuator.properties", "classpath:jeap-tracing.properties"})
@Data
class MonitoringConfig {

    static final String ENABLE_ADMIN_ENDPOINTS_PROPERTY = "jeap.monitor.actuator.enable-admin-endpoints";
    static final String ADDITIONAL_PERMITTED_ENDPOINTS_PROPERTY = "jeap.monitor.actuator.additional-permitted-endpoints";

    @NestedConfigurationProperty
    private PrometheusConfig prometheus;

    @NestedConfigurationProperty
    private ActuatorConfig actuator;

    /**
     * Configuration of the prometheus endpoint
     */
    @Data
    static class PrometheusConfig {
        /**
         * Username to secure the prometheus endpoint
         */
        private String user;
        /**
         * Password to secure the prometheus endpoint. Can be encrpyted
         * (check {@link org.springframework.security.crypto.factory.PasswordEncoderFactories})
         */
        private String password;
        /**
         * Shoud the prometheus endpoint be secured?
         */
        private boolean secure = true;
    }

    @Data
    static class ActuatorConfig {
        /**
         * Username to connect to secured actuator endpoints
         */
        private String user;
        /**
         * Password to connect to secured actuator endpoint
         */
        private String password;
        /**
         * Should the spring boot admin endpoins be enabled? Not allowed in production
         */
        private boolean enableAdminEndpoints = false;
        /**
         * List of permitted actuator endpoints. This setting should not be overwritten,
         * use {@link #additionalPermittedEndpoints} instead
         */
        private Class<?>[] permittedEndpoints = new Class<?>[0];
        /**
         * Additional actuator endpoints to be used with spring boot admin
         */
        private Class<?>[] additionalPermittedEndpoints = new Class<?>[0];
    }
}
