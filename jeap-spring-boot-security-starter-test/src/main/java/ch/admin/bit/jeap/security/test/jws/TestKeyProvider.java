package ch.admin.bit.jeap.security.test.jws;

import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Provides an RSA key pair that can be used to sign Java web tokens and/or to verify the signature of Java web tokens.
 * The key pair provided is read from the keystore key specified by the TestKeyProviderConfigurationProperties
 * instance given. The password of the key is expected to be the same as the password of the keystore. The key alias
 * from the keystore is used as JWK key id.
 */
@RequiredArgsConstructor
public class TestKeyProvider {

    private final TestKeyProviderConfigurationProperties config;
    private final ResourceLoader resourceLoader;

    public RSAKey getAuthServerKey() {
        Resource keystoreResource = resourceLoader.getResource(config.getKeytoreResourcePath());
        return RSAKeyUtils.readRsaKeyPairFromResource(keystoreResource, config.getKeystoreType(),  config.getKeyAlias(), config.getKeytorePassword(), config.getKeyAlias());
    }

}
