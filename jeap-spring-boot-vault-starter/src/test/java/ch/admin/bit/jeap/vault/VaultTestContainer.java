package ch.admin.bit.jeap.vault;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

class VaultTestContainer extends VaultContainer<VaultTestContainer> {

    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("vault:1.12.0")
            .asCompatibleSubstituteFor("vault");

    VaultTestContainer() {
        super(IMAGE_NAME);
    }
}
