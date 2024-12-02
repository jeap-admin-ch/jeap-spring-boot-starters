package ch.admin.bit.jeap.vault;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.vault.VaultContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("SpringBootApplicationProperties")
@Slf4j
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "jeap.vault.url=http://localhost:28282")
class VaultTestcontainersBaseIT {

    @Autowired
    private VaultTestConfiguration vaultTestConfiguration;

    @Container
    static public VaultContainer<?> vaultContainer = new VaultTestContainer()
            .withVaultToken("secret")
            .withExtraHost("vault-server", "127.0.0.1")
            .withCopyFileToContainer(MountableFile.forHostPath("docker/vault-test-config.sh"), "/vault-test-config.sh")
            .withCopyFileToContainer(MountableFile.forHostPath("docker/jeap-spring-boot-vault-starter-pol.hcl"), "/jeap-spring-boot-vault-starter-pol.hcl");

    void contextLoadsWithSecretsInjectedFromVaultInTestcontainer() {
        assertEquals("vault-secret-value", vaultTestConfiguration.getTestSecret());
    }

    @SneakyThrows
    static void init(boolean bootstrapEnabled)  {
        log.info("Enabling bootstrap context: {}", bootstrapEnabled);
        System.setProperty("spring.cloud.bootstrap.enabled", Boolean.toString(bootstrapEnabled));
        ExecResult execResult = vaultContainer.execInContainer("/vault-test-config.sh");
        log.info("Test config stdout: {}", execResult.getStdout());
        log.info("Test config stderr: {}", execResult.getStderr());
        assertEquals(0, execResult.getExitCode(), "Vault config was successful");
    }

    @AfterAll
    static void reset() {
        System.clearProperty("spring.cloud.bootstrap.enabled");
    }
}
