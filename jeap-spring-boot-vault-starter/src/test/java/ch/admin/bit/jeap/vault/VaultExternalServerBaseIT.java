package ch.admin.bit.jeap.vault;

import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("SpringBootApplicationProperties")
@SpringBootTest(properties = "jeap.vault.url=http://vault-server:8200")
@EnabledIf("#{systemProperties['pipeline.build.step.integrationTest'] != null}")
public class VaultExternalServerBaseIT {

    @Autowired
    private VaultTestConfiguration vaultTestConfiguration;

    void contextLoadsWithSecretsInjectedFromStandaloneVaultServer() {
        assertEquals("vault-secret-value", vaultTestConfiguration.getTestSecret());
    }

    static void init(boolean bootstrapEnabled)  {
        System.setProperty("spring.cloud.bootstrap.enabled", Boolean.toString(bootstrapEnabled));
    }

    @AfterAll
    static void reset() {
        System.clearProperty("spring.cloud.bootstrap.enabled");
    }

}
