package ch.admin.bit.jeap.config.aws.appconfig.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("jeap.appconfig.test.configprops")
class ConfigProperties {

    private String property;

}
