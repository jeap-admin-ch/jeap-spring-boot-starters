package ch.admin.bit.jeap.security.resource.claimsetconverter;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EiamClaimSetConverterTest {

    private final EiamClaimSetConverter converter = new EiamClaimSetConverter();

    @Test
    void doConvert_shouldRenameRoleToUserroles() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", List.of("role-a", "role-b"));

        Map<String, Object> result = converter.doConvert(claims);

        assertThat(result)
                .containsEntry("userroles", List.of("role-a", "role-b"))
                .doesNotContainKey("role");
    }

    @Test
    void doConvert_shouldRenameUserExtIdToExtId() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userExtId", "ext-123");

        Map<String, Object> result = converter.doConvert(claims);

        assertThat(result)
                .containsEntry("ext_id", "ext-123")
                .doesNotContainKey("userExtId");
    }

    @Test
    void doConvert_shouldMapLanguageToUpperCaseLocale() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("language", "de");

        Map<String, Object> result = converter.doConvert(claims);

        assertThat(result)
                .containsEntry("locale", "DE")
                .doesNotContainKey("language");
    }

    @Test
    void doConvert_shouldSetCtxToUser() {
        Map<String, Object> claims = new HashMap<>();

        Map<String, Object> result = converter.doConvert(claims);

        assertThat(result).containsEntry("ctx", "USER");
    }

    @Test
    void doConvert_shouldLeaveOtherClaimsUnchanged() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user123");
        claims.put("iss", "https://issuer.example.com");
        claims.put("custom", "value");
        claims.put("role", List.of("admin"));
        claims.put("userExtId", "ext-123");
        claims.put("language", "fr");

        Map<String, Object> result = converter.doConvert(claims);

        assertThat(result)
                .containsEntry("sub", "user123")
                .containsEntry("iss", "https://issuer.example.com")
                .containsEntry("custom", "value");
    }

    @Test
    void doConvert_shouldNotFailWhenOptionalClaimsAbsent() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user123");

        Map<String, Object> result = converter.doConvert(claims);

        assertThat(result)
                .containsEntry("sub", "user123")
                .containsEntry("ctx", "USER")
                .doesNotContainKey("userroles")
                .doesNotContainKey("ext_id")
                .doesNotContainKey("locale");
    }

}
