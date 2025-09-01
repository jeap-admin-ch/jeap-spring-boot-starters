package ch.admin.bit.jeap.vault;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("local") // note: no vault profile selected
public class NoVaultIT {

    @Autowired
    private VaultTestConfiguration vaultTestConfiguration;

    @Test
    void contextLoadsWithSecretsInjectedFromLocalPropertyFile() {
        assertEquals("local-secret-value", vaultTestConfiguration.getTestSecret());
    }

}
