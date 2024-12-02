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

class VaultBasePropertiesEnvPostProcessorTest {

    private final DeferredLogFactory deferredLogFactory = mock(DeferredLogFactory.class);

    @BeforeEach
    void setUp() {
        when(deferredLogFactory.getLog((Class<?>) any())).thenAnswer(invocation ->
                LogFactory.getLog(invocation.getArgument(0).getClass()));
    }

    @Test
    void postProcessEnvironment_whenVaultEnabled_thenShouldLoadBaseProperties() {
        VaultBasePropertiesEnvPostProcessor basePropsEnvPostProcessor = new VaultBasePropertiesEnvPostProcessor(deferredLogFactory);
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.cloud.vault.enabled", "true");
        env.setProperty("jeap.vault.url", "http://foo");

        // when the post processor is applied
        basePropsEnvPostProcessor.postProcessEnvironment(env, null);

        // then vault base configuration should be set
        assertEquals("http://foo", env.getProperty("spring.cloud.vault.uri"));
        assertEquals("true", env.getProperty("spring.cloud.vault.kv.enabled"));

        // when the environment contains a property value and the post processor is applied again
        env.setProperty("spring.cloud.vault.uri", "http://updated");
        basePropsEnvPostProcessor.postProcessEnvironment(env, null);

        // then it should not overwrite the property value in the environment
        assertEquals("http://updated", env.getProperty("spring.cloud.vault.uri"));
    }

    @Test
    void postProcessEnvironment_whenVaultEnabledAndKubernetes_thenShouldLoadRhosBaseProperties() {
        VaultBasePropertiesEnvPostProcessor basePropsEnvPostProcessor = new VaultBasePropertiesEnvPostProcessor(deferredLogFactory);
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.cloud.vault.enabled", "true");
        env.setProperty("jeap.vault.url", "http://foo");
        env.setProperty("spring.main.cloud-platform", CloudPlatform.KUBERNETES.toString());

        // when the post processor is applied
        basePropsEnvPostProcessor.postProcessEnvironment(env, null);

        // then vault base configuration should be set for Kubernetes/RHOS
        assertEquals("http://foo", env.getProperty("spring.cloud.vault.uri"));
        assertEquals("true", env.getProperty("spring.cloud.vault.kv.enabled"));
        assertEquals("KUBERNETES", env.getProperty("spring.cloud.vault.authentication"));

        // and not set for app-role
        assertNull(env.getProperty("spring.cloud.vault.app-role.role-id"));
        assertNull(env.getProperty("spring.cloud.vault.app-role.app-role-path"));

        // when the environment contains a property value and the post processor is applied again
        env.setProperty("spring.cloud.vault.uri", "http://updated");
        basePropsEnvPostProcessor.postProcessEnvironment(env, null);

        // then it should not overwrite the property value in the environment
        assertEquals("http://updated", env.getProperty("spring.cloud.vault.uri"));
    }

    @Test
    void postProcessEnvironment_whenVaultDisabled_thenShouldNotLoadBaseProperties() {
        VaultBasePropertiesEnvPostProcessor basePropsEnvPostProcessor = new VaultBasePropertiesEnvPostProcessor(deferredLogFactory);
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.cloud.vault.enabled", "false");

        // when the post processor is applied
        basePropsEnvPostProcessor.postProcessEnvironment(env, null);

        // then vault base configuration should not have been sett
        assertNull(env.getProperty("spring.cloud.vault.kv.enabled"));
    }

}