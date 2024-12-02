package ch.admin.bit.jeap.vault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(VaultTestConfiguration.class)
public class VaultTestApp {

    public static void main(String[] args) {
        SpringApplication.run(VaultTestApp.class, args);
    }
}
