package ch.admin.bit.jeap.vault;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

class VaultTestContainer extends VaultContainer<VaultTestContainer> {

    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("vault:1.12.0")
            .asCompatibleSubstituteFor("vault");

    VaultTestContainer() {
        super(IMAGE_NAME);
        // Vault configuration properties must be part of the bootstrap application context, as they are used to load
        // properties to create the actual spring application context. Dynamically adding bootstrap properties
        // (i.e. a random vault container port) in spring boot tests is extremely cumbersome. Thus using a fixed port
        // here.
        addFixedExposedPort(28282, 8200);
    }
}
