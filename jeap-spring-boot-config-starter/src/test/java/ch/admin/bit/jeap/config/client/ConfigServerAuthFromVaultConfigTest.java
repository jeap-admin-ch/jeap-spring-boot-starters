package ch.admin.bit.jeap.config.client;

import ch.admin.bit.jeap.config.client.ConfigServerAuthFromVaultConfig.ActivationCondition;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

class ConfigServerAuthFromVaultConfigTest {

    private static final String USERNAME_PROPERTY = "jeap.config.client.vault.config-server.username.property";
    private static final String PASSWORD_PROPERTY = "jeap.config.client.vault.config-server.password.property";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "secret";
    private static final String CONFIG_SERVER_USERNAME_PROPERTY = "spring.cloud.config.username";
    private static final String CONFIG_SERVER_PASSWORD_PROPERTY = "spring.cloud.config.password";

    @Test
    void test_WhenPropertyNamesAndValuesInVaultConfigured_ThenConfigServerCredentialsConfigured() {
        ConfigurableEnvironment env = mockConfigurableEnvironment(USERNAME_PROPERTY, PASSWORD_PROPERTY);
        PropertySourceLocator locator = mockPropertySourceLocator(USERNAME_PROPERTY, PASSWORD_PROPERTY, USERNAME, PASSWORD);
        ConfigClientProperties configClientProperties = new ConfigClientProperties(env);

        new ConfigServerAuthFromVaultConfig(locator, env,configClientProperties );

        assertThat(env.getProperty(CONFIG_SERVER_USERNAME_PROPERTY)).isEqualTo(USERNAME);
        assertThat(env.getProperty(CONFIG_SERVER_PASSWORD_PROPERTY)).isEqualTo(PASSWORD);
        assertThat(configClientProperties.getUsername()).isEqualTo(USERNAME);
        assertThat(configClientProperties.getPassword()).isEqualTo(PASSWORD);
    }

    @Test
    void test_WhenPropertyNamesConfiguredAndValuesInVaultNotConfigured_ThenThrowsException() {
        ConfigurableEnvironment env = mockConfigurableEnvironment(USERNAME_PROPERTY, PASSWORD_PROPERTY);
        PropertySourceLocator locator = mockPropertySourceLocator(USERNAME_PROPERTY, PASSWORD_PROPERTY, null, null);

        assertThatThrownBy(
                () -> new ConfigServerAuthFromVaultConfig(locator, env, new ConfigClientProperties(env))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void test_WhenPropertyNamesConfiguredAndUsernameValueInVaultNotConfigured_ThenThrowsException() {
        ConfigurableEnvironment env = mockConfigurableEnvironment(USERNAME_PROPERTY, PASSWORD_PROPERTY);
        PropertySourceLocator locator = mockPropertySourceLocator(USERNAME_PROPERTY, PASSWORD_PROPERTY, null, PASSWORD);

        assertThatThrownBy(
                () -> new ConfigServerAuthFromVaultConfig(locator, env, new ConfigClientProperties(env))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void test_WhenPropertyNamesConfiguredAndPasswordValueInVaultNotConfigured_ThenThrowsException() {
        ConfigurableEnvironment env = mockConfigurableEnvironment(USERNAME_PROPERTY, PASSWORD_PROPERTY);
        PropertySourceLocator locator = mockPropertySourceLocator(USERNAME_PROPERTY, PASSWORD_PROPERTY, USERNAME, null);

        assertThatThrownBy(
                () -> new ConfigServerAuthFromVaultConfig(locator, env, new ConfigClientProperties(env))
        ).isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    void test_WhenPropertyNamesConfigured_ThenActivationConditionMet() {
        ConditionContext context = mockConditionContext(USERNAME_PROPERTY, PASSWORD_PROPERTY);
        AnnotatedTypeMetadata metadata = mockAnnotatedTypeMetadata();
        ActivationCondition condition = new ActivationCondition();

        boolean isConditionMet =condition.matches(context, metadata);

        assertThat(isConditionMet).isTrue();
    }

    @Test
    void test_WhenPasswordPropertyNameNotConfigured_ThenActivationConditionNotMet() {
        ConditionContext context = mockConditionContext(USERNAME_PROPERTY, null);
        AnnotatedTypeMetadata metadata = mockAnnotatedTypeMetadata();
        ActivationCondition condition = new ActivationCondition();

        boolean isConditionMet =condition.matches(context, metadata);

        assertThat(isConditionMet).isFalse();
    }

    @Test
    void test_WhenUsernamePropertyNameNotConfigured_ThenActivationConditionNotMet() {
        ConditionContext context = mockConditionContext(null, PASSWORD_PROPERTY);
        AnnotatedTypeMetadata metadata = mockAnnotatedTypeMetadata();
        ActivationCondition condition = new ActivationCondition();

        boolean isConditionMet =condition.matches(context, metadata);

        assertThat(isConditionMet).isFalse();
    }

    @Test
    void test_WhenNoPropertyNameNotConfigured_ThenActivationConditionNotMet() {
        ConditionContext context = mockConditionContext(null, null);
        AnnotatedTypeMetadata metadata = mockAnnotatedTypeMetadata();
        ActivationCondition condition = new ActivationCondition();

        boolean isConditionMet =condition.matches(context, metadata);

        assertThat(isConditionMet).isFalse();
    }

    private ConfigurableEnvironment mockConfigurableEnvironment(String usernameProperty, String passwordProperty) {
        MockEnvironment environment = new MockEnvironment();
        if (usernameProperty != null) {
            environment.withProperty("jeap.config.client.vault.config-server.username.property", usernameProperty);
        }
        if (passwordProperty != null) {
            environment.withProperty("jeap.config.client.vault.config-server.password.property", passwordProperty);
        }
        // The following values should be overridden later by the values from vault
        environment.
                withProperty(CONFIG_SERVER_USERNAME_PROPERTY, "some-preconfigured-username").
                withProperty(CONFIG_SERVER_PASSWORD_PROPERTY, "some-preconfigured-password");
        return environment;
    }

    @SuppressWarnings({"rawtypes", "SameParameterValue", "unchecked"})
    private PropertySourceLocator mockPropertySourceLocator(String usernameProperty, String passwordProperty, String username, String password)  {
        PropertySourceLocator locator = Mockito.mock(PropertySourceLocator.class);
        PropertySource source = Mockito.mock(PropertySource.class);
        Mockito.when(locator.locate(any(Environment.class))).thenReturn(source);
        if (username != null) {
            Mockito.when(source.getProperty(usernameProperty)).thenReturn(username);
        }
        if (password != null) {
            Mockito.when(source.getProperty(passwordProperty)).thenReturn(password);
        }
        return locator;
    }

    private ConditionContext mockConditionContext(String usernameProperty, String passwordProperty) {
        ConfigurableEnvironment env = mockConfigurableEnvironment(usernameProperty, passwordProperty);
        ConditionContext context = Mockito.mock(ConditionContext.class);
        Mockito.when(context.getEnvironment()).thenReturn(env);
        return context;
    }

    private AnnotatedTypeMetadata mockAnnotatedTypeMetadata() {
        return Mockito.mock(AnnotatedTypeMetadata.class);
    }
}
