package ch.admin.bit.jeap.config.client;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;

@AutoConfiguration
public class ConfigurationGuard implements InitializingBean {

    @Value("${spring.cloud.bus.destination:#{null}}")
    private String springCloudBusDestination;

    @Value("${jeap.config.client.enabled:true}")
    private Boolean configClientEnabled;

    @Override
    public void afterPropertiesSet() {
        if ((this.springCloudBusDestination == null) && (configClientEnabled)) {
            throw new IllegalArgumentException("'spring.cloud.bus.destination' must be configured with a Kafka topic name");
        }
    }
}
