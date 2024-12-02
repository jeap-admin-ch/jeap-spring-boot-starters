package ch.admin.bit.jeap.vault;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;

// Inject secret test.testSecret into this class from vault
@ConfigurationProperties("test")
@Data
@Slf4j
public class VaultTestConfiguration {

    private String testSecret;

    @PostConstruct
    public void verifyConfiguration() {
        log.info("Secret injected: {}", testSecret);
    }
}
