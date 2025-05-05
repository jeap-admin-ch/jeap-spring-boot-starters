package ch.admin.bit.jeap.monitor;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class NoUpgradeEncodingDelegatingPasswordEncoderTest {


    @Test
    void testPassword() {
        String originalPassword = "{noop}asf4364659888685465";
        PasswordEncoder passwordEncoder = NoUpgradeEncodingDelegatingPasswordEncoder.createInstance();

        assertFalse(passwordEncoder.upgradeEncoding(originalPassword));
    }

    @Test
    void encodingLongPasswordsToBCryptIsForbidden() {
        String originalPassword = "{noop}asf436465988868546545435466879809898989891234asf436465988868546545435466879809898989891234asf436465988868546545435466879809898989891234";
        PasswordEncoder passwordEncoder = NoUpgradeEncodingDelegatingPasswordEncoder.createInstance();

        assertThrows(IllegalArgumentException.class, () -> passwordEncoder.encode(originalPassword), "password cannot be more than 72 bytes");
    }

}