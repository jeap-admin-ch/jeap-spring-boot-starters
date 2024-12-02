package ch.admin.bit.jeap.config.aws.appconfig.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

@Data
class StandardScopedBean {

    @Value("${jeap.appconfig.test.standardscope.property:default}")
    private String property;

}