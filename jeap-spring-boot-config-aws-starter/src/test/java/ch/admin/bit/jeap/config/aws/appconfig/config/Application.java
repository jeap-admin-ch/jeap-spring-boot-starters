package ch.admin.bit.jeap.config.aws.appconfig.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableConfigurationProperties({ConfigProperties.class})
class Application {

    @Bean
    DummyService dummyService(ConfigProperties configProperties, RefreshScopedBean refreshScopedBean, StandardScopedBean standardScopedBean) {
        return new DummyService(configProperties, refreshScopedBean, standardScopedBean);
    }

    @Bean
    @RefreshScope
    RefreshScopedBean refreshScopedBean() {
            return new RefreshScopedBean();
        }

    @Bean
    StandardScopedBean standardScopedBean() {
        return new StandardScopedBean();
    }

    @Bean
    KeepContextAliveRunner keepContextAlive() {
        return new KeepContextAliveRunner();
    }

}