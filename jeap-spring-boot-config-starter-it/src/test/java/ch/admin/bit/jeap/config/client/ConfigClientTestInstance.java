package ch.admin.bit.jeap.config.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class ConfigClientTestInstance {

    public static void main(Map<String,Object> propertiesMap) {
        String[] args = {};
        SpringApplication sa = new SpringApplication(ConfigClientTestInstance.class);
        sa.setDefaultProperties(propertiesMap);
        sa.run(args);
    }
}
