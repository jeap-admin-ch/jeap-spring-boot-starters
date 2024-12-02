package ch.admin.bit.jeap.vault;

import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.mock.env.MockEnvironment;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VaultDefaultPropertiesEnvPostProcessorTest {

    private final DeferredLogFactory deferredLogFactory = mock(DeferredLogFactory.class);

    @BeforeEach
    void setUp() {
        when(deferredLogFactory.getLog((Class<?>) any())).thenAnswer(invocation ->
                LogFactory.getLog(invocation.getArgument(0).getClass()));
    }

    @Test
    void postProcessEnvironment_whenVaultEnabled_thenShouldLoadDefaultProperties() {
        VaultDefaultPropertiesEnvPostProcessor defaultPropsEnvPostProcessor = new VaultDefaultPropertiesEnvPostProcessor(deferredLogFactory);
        MockEnvironment env = initEnvironmentForDefaults();
        env.setProperty("spring.cloud.vault.enabled", "true");

        // when the post processor is applied
        defaultPropsEnvPostProcessor.postProcessEnvironment(env, null);

        // then the vault default configuration should be set
        assertEquals("test-role-id", env.getProperty("jeap.vault.app-role.role-id"));
    }

    @Test
    void postProcessEnvironment_whenVaultDisabled_thenShouldNotLoadDefaultProperties() {
        VaultDefaultPropertiesEnvPostProcessor defaultPropsEnvPostProcessor = new VaultDefaultPropertiesEnvPostProcessor(deferredLogFactory);
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.cloud.vault.enabled", "false");

        // when the post processor is applied
        defaultPropsEnvPostProcessor.postProcessEnvironment(env, null);

        // then the vault default configuration should not be set
        assertNull(env.getProperty("jeap.vault.app-role.role-id"));
    }

    @Test
    void postProcessEnvironment_whenKubernetes_thenShouldNotLoadDefaultProperties() {
        VaultDefaultPropertiesEnvPostProcessor defaultPropsEnvPostProcessor = new VaultDefaultPropertiesEnvPostProcessor(deferredLogFactory);
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.cloud.vault.enabled", "true");
        env.setProperty("spring.main.cloud-platform", CloudPlatform.KUBERNETES.toString());

        // when the post processor is applied
        defaultPropsEnvPostProcessor.postProcessEnvironment(env, null);

        // then the vault default configuration should not be set
        assertNull(env.getProperty("jeap.vault.app-role.role-id"));
    }

    private MockEnvironment initEnvironmentForDefaults() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.cloud.vault.enabled", "true");
        environment.setProperty("spring.application.name", "test-app");
        environment.setProperty("vcap.services.test-app-approle.credentials.role-id", "test-role-id");
        return environment;
    }

}