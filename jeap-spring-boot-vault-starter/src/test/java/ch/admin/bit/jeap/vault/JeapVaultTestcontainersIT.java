package ch.admin.bit.jeap.vault;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"jeap-vault", "import"})
@SuppressWarnings("java:S2699") // asserts are in the super class method, but sonar does not get it
class JeapVaultTestcontainersIT extends VaultTestcontainersBaseIT {

    @Test
    void contextLoadsWithSecretsInjectedFromVaultInTestcontainer() {
        super.contextLoadsWithSecretsInjectedFromVaultInTestcontainer();
    }

}
