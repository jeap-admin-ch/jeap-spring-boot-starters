package ch.admin.bit.jeap.security.test.resource.extension;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class WithAuthenticationExtensionTest {

    // some user roles
    private static final String UR_1 = "ur1";
    private static final String UR_2 = "ur2";

    // some business partner ids
    private static final String BP_1 = "bp1";

    // some business partner roles
    private static final String BPR_1 = "bpr1";
    private static final String BPR_2 = "bpr2";

    // some user data
    private static final String NAME = "Max Muster";
    private static final String GIVEN_NAME = "Max";
    private static final String FAMILY_NAME = "Muster";
    private static final String PREFERRED_USERNAME = "max_muster";
    private static final String LOCALE_DE = "DE";
    private static final String SUBJECT = "69368608-D736-43C8-5F76-55B7BF168299";
    private static final String EXT_ID = "287365";
    private static final String ADMIN_DIR_UID = "U11111111";

    private static final AnonymousAuthenticationToken PREVIOUS_AUTHENTICATION  =
            new AnonymousAuthenticationToken("key", "principal", Collections.singleton(new SimpleGrantedAuthority("role")));

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(PREVIOUS_AUTHENTICATION);
    }

    @AfterEach
    void tearDown() {
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(PREVIOUS_AUTHENTICATION);
    }

    @Test
    @WithAuthentication("createAuthentication")
    void testWithAuthentication() {
        assertAuthentication();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    @WithAuthentication("createAuthentication")
    void testParameterizedWithAuthentication(int i) {
        assertAuthentication();
    }

    private void assertAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication instanceof JeapAuthenticationToken).isTrue();
        JeapAuthenticationToken jeapAuthenticationToken = (JeapAuthenticationToken) authentication;
        assertThat(jeapAuthenticationToken.getUserRoles()).containsOnly(UR_1, UR_2);
        assertThat(jeapAuthenticationToken.getBusinessPartnerRoles().keySet()).containsOnly(BP_1);
        assertThat(jeapAuthenticationToken.getBusinessPartnerRoles().get(BP_1)).containsOnly(BPR_1, BPR_2);
        assertThat(jeapAuthenticationToken.getTokenExtId()).isEqualTo(EXT_ID);
        assertThat(jeapAuthenticationToken.getAdminDirUID()).isEqualTo(ADMIN_DIR_UID);
        assertThat(jeapAuthenticationToken.getTokenName()).isEqualTo(NAME);
        assertThat(jeapAuthenticationToken.getTokenGivenName()).isEqualTo(GIVEN_NAME);
        assertThat(jeapAuthenticationToken.getTokenFamilyName()).isEqualTo(FAMILY_NAME);
        assertThat(jeapAuthenticationToken.getPreferredUsername()).isEqualTo(PREFERRED_USERNAME);
        assertThat(jeapAuthenticationToken.getTokenLocale()).isEqualTo(LOCALE_DE);
        assertThat(jeapAuthenticationToken.getTokenSubject()).isEqualTo(SUBJECT);
    }

    private JeapAuthenticationToken createAuthentication() {
        return JeapAuthenticationTestTokenBuilder.create().
                withUserRoles(UR_1, UR_2).
                withBusinessPartnerRoles(BP_1, BPR_1, BPR_2).
                withExtId(EXT_ID).
                withAdminDirUID(ADMIN_DIR_UID).
                withName(NAME).
                withGivenName(GIVEN_NAME).
                withFamilyName(FAMILY_NAME).
                withPreferredUsername(PREFERRED_USERNAME).
                withLocale(LOCALE_DE).
                withSubject(SUBJECT).
                build();
    }

}
