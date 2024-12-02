package ch.admin.bit.jeap.config.aws.appconfig.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

@Data
class RefreshScopedBean {

    @Value("${jeap.appconfig.test.refreshscope.property:default}")
    private String property;

}
