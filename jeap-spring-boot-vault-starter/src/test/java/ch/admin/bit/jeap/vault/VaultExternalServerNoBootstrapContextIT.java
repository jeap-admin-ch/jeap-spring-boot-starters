package ch.admin.bit.jeap.vault;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"vault", "import"})
@SuppressWarnings("java:S2699") // asserts are in the super class method, but sonar does not get it
public class VaultExternalServerNoBootstrapContextIT extends VaultExternalServerBaseIT {

    @Test
    void contextLoadsWithSecretsInjectedFromStandaloneVaultServer() {
        super.contextLoadsWithSecretsInjectedFromStandaloneVaultServer();
    }

    @BeforeAll
    static void init()  {
        init(false);
    }

}