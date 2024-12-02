package ch.admin.bit.jeap.security.test.jws;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties specifying access to a key in a key store. The default values reference an RSA key pair
 * in the resource file (default-rsa-test-key-pair.p12). Therefore a default test key pair is used when no configuration
 * is given.
 */
@Data
@ConfigurationProperties("jeap.oauth2.test-key-provider.auth-server-key")
@SuppressWarnings("squid:S2068") // The password just protects a non-sensitive test key used in local integration tests.
public class TestKeyProviderConfigurationProperties {
    private String keytoreResourcePath = "classpath:/testkeys/default-rsa-test-key-pair.p12";
    private String keystoreType = "pkcs12";
    private String keytorePassword = "secret";
    private String keyAlias = "default-test-key";
}
