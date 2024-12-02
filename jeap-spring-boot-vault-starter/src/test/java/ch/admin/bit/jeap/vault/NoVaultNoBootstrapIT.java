package ch.admin.bit.jeap.vault;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("local")
public class NoVaultNoBootstrapIT {

    @Autowired
    private VaultTestConfiguration vaultTestConfiguration;

    @Test
    void contextLoadsWithSecretsInjectedFromLocalPropertyFile() {
        assertEquals("local-secret-value", vaultTestConfiguration.getTestSecret());
    }

    @BeforeAll
    static void init()  {
        System.setProperty("spring.cloud.bootstrap.enabled", "false");
    }

    @AfterAll
    static void reset() {
        System.clearProperty("spring.cloud.bootstrap.enabled");
    }
}
