package ch.admin.bit.jeap.config.aws.appconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static ch.admin.bit.jeap.config.aws.appconfig.JeapSpringApplicationProperties.SPRING_APPLICATION_CONFIG_PREFIX;

@Data
@ConfigurationProperties(SPRING_APPLICATION_CONFIG_PREFIX)
public class JeapSpringApplicationProperties {

    public static final String SPRING_APPLICATION_CONFIG_PREFIX = "spring.application";

    private String name;

}
