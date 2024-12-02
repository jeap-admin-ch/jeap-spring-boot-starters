package ch.admin.bit.jeap.vault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.ClassUtils;
import org.springframework.vault.client.ClientHttpRequestFactoryFactory;

import java.util.Map;
import java.util.Set;

/**
 * By default, Spring Vault uses a apache commons http client with the default cookiespec. This does not play well with
 * the cookie expiry format on cookies returned by the web application firewall which is fronting the BIT vault
 * instances. As configuring the cookie spec used by the spring vault HTTP client is unfeasible without duplication
 * most of its configuration in spring cloud vault, such log statements are simply suppressed if vault is active.
 */
public class SuppressInvalidCookieHeaderWarningEnvPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (isVaultProfileActive(environment.getActiveProfiles()) && apacheHttpClientActiveForVault()) {
            Map<String, Object> map = Map.of(
                    "logging.level.org.apache.http.client.protocol",
                    "ERROR"
            );
            environment.getPropertySources().addLast(new MapPropertySource(getClass().getSimpleName(), map));
        }
    }

    /**
     * Reflects the logic in ClientHttpRequestFactoryFactory#HTTP_COMPONENTS_PRESENT
     */
    private boolean apacheHttpClientActiveForVault() {
        return ClassUtils.isPresent("org.apache.http.client.HttpClient",
                ClientHttpRequestFactoryFactory.class.getClassLoader());
    }

    private boolean isVaultProfileActive(String[] activeProfiles) {
        return activeProfiles != null &&
                (Set.of(activeProfiles).contains("vault") || Set.of(activeProfiles).contains("jeap-vault"));
    }
}
