package ch.admin.bit.jeap.security.test.jws;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.encrypt.KeyStoreKeyFactory;

import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Utility class for creating and loading RSAKey JWK instances.
 */
public class RSAKeyUtils {

    private RSAKeyUtils() {
        // static utility class
    }

    /***
     * Read an RSA key pair from a key store into a RSAKey JWK instance.
     * To create such a key pair the following keytool command can be used (adjust validity, alias, store name etc. as needed):
     * keytool -genkeypair -keysize 2048 -validity 20000 -keyalg RSA -alias default-test-key -storetype pkcs12 -keystore default-rsa-test-key-pair.p12
     *
     * @param rsaKeyPairResource The key store resource
     * @param keystoreType The keystore type (e.g. "pksc12", "jks", etc.)
     * @param alias The alias of the RSA key pair in the key store
     * @param password The passwort of the key store and the RSA key pair in the key store
     * @param jwkKeyId The JWK key id to assign to the key pair read from the store
     * @return A RSAKey JWK instance read from the referenced RSA key pair in the given pkcs12 store
     */
    public static RSAKey readRsaKeyPairFromResource(Resource rsaKeyPairResource, String keystoreType, String alias, String password, String jwkKeyId) {
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(rsaKeyPairResource, password.toCharArray(), keystoreType);
        KeyPair keyPair = keyStoreKeyFactory.getKeyPair(alias, password.toCharArray());
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic()).
                          privateKey((RSAPrivateKey) keyPair.getPrivate()).
                          keyID(jwkKeyId).
                          build();
    }

    /**
     * Create a new RSA key pair.
     *
     * @return The RSA key pair as RSAKey JWK instance.
     */
    public static RSAKey createRsaKeyPair() {
        try {
            return new RSAKeyGenerator(2048).keyID("auto-generated-by-JwsBuilder").generate();
        } catch (JOSEException e) {
            throw new IllegalStateException("Unexpected JOSE Exception", e);
        }
    }

}
