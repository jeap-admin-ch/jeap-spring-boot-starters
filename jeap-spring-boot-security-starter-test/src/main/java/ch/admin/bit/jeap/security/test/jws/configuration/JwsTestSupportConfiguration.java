package ch.admin.bit.jeap.security.test.jws.configuration;

import ch.admin.bit.jeap.security.test.jws.JwsBuilderFactory;
import ch.admin.bit.jeap.security.test.jws.TestKeyProvider;
import ch.admin.bit.jeap.security.test.jws.TestKeyProviderConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
@EnableConfigurationProperties(TestKeyProviderConfigurationProperties.class)
public class JwsTestSupportConfiguration {

    @Bean
    public TestKeyProvider testKeyProvider(TestKeyProviderConfigurationProperties config, ResourceLoader resourceLoader) {
        return new TestKeyProvider(config, resourceLoader);
    }

    @Bean
    public JwsBuilderFactory jwsBuilderFactory(TestKeyProvider testKeyProvider) {
        return new JwsBuilderFactory(testKeyProvider);
    }

}
