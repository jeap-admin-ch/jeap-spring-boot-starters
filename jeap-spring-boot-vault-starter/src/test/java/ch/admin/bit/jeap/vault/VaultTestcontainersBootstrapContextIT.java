package ch.admin.bit.jeap.vault;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("vault")
@SuppressWarnings("java:S2699") // asserts are in the super class method, but sonar does not get it
class VaultTestcontainersBootstrapContextIT extends VaultTestcontainersBaseIT {

    @BeforeAll
    static void init() {
        init(true);
    }

    @Test
    void contextLoadsWithSecretsInjectedFromVaultInTestcontainer() {
        super.contextLoadsWithSecretsInjectedFromVaultInTestcontainer();
    }

}
