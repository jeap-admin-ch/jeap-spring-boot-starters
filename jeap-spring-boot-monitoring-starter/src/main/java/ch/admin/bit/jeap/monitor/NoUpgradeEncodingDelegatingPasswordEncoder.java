package ch.admin.bit.jeap.monitor;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * The default {@link DelegatingPasswordEncoder} implementation always re-encodes passwords that were originally encoded
 * with a different algorithm than the chosen default {@link DelegatingPasswordEncoder#upgradeEncoding(String)}.
 * <p>
 * This implementation overrides this behavior and avoids the re-encoding of the password for situations where this
 * operation is not desired, such as when passwords are externalized.
 * <p>
 * Without re-encoding, existing chosen passwords won't cause regression failures when the implementation of password
 * encoders is changed.
 */
public class NoUpgradeEncodingDelegatingPasswordEncoder extends DelegatingPasswordEncoder {

    public NoUpgradeEncodingDelegatingPasswordEncoder(String idForEncode, Map<String, PasswordEncoder> idToPasswordEncoder) {
        super(idForEncode, idToPasswordEncoder);
    }

    @Override
    public boolean upgradeEncoding(String prefixEncodedPassword) {
        return false;
    }

    /**
     * Creates a new instance of {@link PasswordEncoder} with the default encoders.
     * Configuration is taken from {@link org.springframework.security.crypto.factory.PasswordEncoderFactories#createDelegatingPasswordEncoder()}
     *
     * @return a new instance of {@link PasswordEncoder}
     */
    @SuppressWarnings("deprecation")
    public static PasswordEncoder createInstance() {
        String encodingId = "bcrypt";
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put(encodingId, new BCryptPasswordEncoder());
        encoders.put("ldap", new org.springframework.security.crypto.password.LdapShaPasswordEncoder());
        encoders.put("MD4", new org.springframework.security.crypto.password.Md4PasswordEncoder());
        encoders.put("MD5", new org.springframework.security.crypto.password.MessageDigestPasswordEncoder("MD5"));
        encoders.put("noop", org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance());
        encoders.put("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_5());
        encoders.put("pbkdf2@SpringSecurity_v5_8", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("scrypt", SCryptPasswordEncoder.defaultsForSpringSecurity_v4_1());
        encoders.put("scrypt@SpringSecurity_v5_8", SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("SHA-1", new org.springframework.security.crypto.password.MessageDigestPasswordEncoder("SHA-1"));
        encoders.put("SHA-256",
                new org.springframework.security.crypto.password.MessageDigestPasswordEncoder("SHA-256"));
        encoders.put("sha256", new org.springframework.security.crypto.password.StandardPasswordEncoder());
        encoders.put("argon2", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_2());
        encoders.put("argon2@SpringSecurity_v5_8", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        return new NoUpgradeEncodingDelegatingPasswordEncoder(encodingId, encoders);
    }

}


