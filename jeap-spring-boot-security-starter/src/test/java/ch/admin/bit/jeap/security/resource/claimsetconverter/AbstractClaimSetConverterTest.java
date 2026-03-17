package ch.admin.bit.jeap.security.resource.claimsetconverter;

import org.junit.jupiter.api.Test;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractClaimSetConverterTest {

    /**
     * Concrete test subclass that simply passes claims through unchanged.
     */
    static class TestClaimSetConverter extends AbstractClaimSetConverter {
        @Override
        protected Map<String, Object> doConvert(@NonNull Map<String, Object> claims) {
            return claims;
        }
    }

    private final TestClaimSetConverter converter = new TestClaimSetConverter();

    @Test
    @SuppressWarnings("DataFlowIssue")
    void convert_shouldApplyDefaultSpringSecurityMappings() {
        Instant issuedAt = Instant.ofEpochSecond(1700000000L);
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user123");
        claims.put("iat", issuedAt);
        claims.put("custom", "value");

        Map<String, Object> result = converter.convert(claims);

        assertThat(result)
                .containsEntry("sub", "user123")
                .containsEntry("custom", "value")
                .containsKey("iat");
        Object iat = result.get("iat");
        // iat is expected to have been mapped to an Instant by the default Spring Security claim set converter
        assertThat(iat)
                .isInstanceOf(Instant.class)
                .isEqualTo(issuedAt);
    }

    @Test
    void renameClaim_shouldRenameExistingClaim() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("oldName", "theValue");

        converter.renameClaim("oldName", "newName", claims);

        assertThat(claims)
                .containsEntry("newName", "theValue")
                .doesNotContainKey("oldName");
    }

    @Test
    void renameClaim_shouldDoNothingWhenClaimAbsent() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("other", "value");

        converter.renameClaim("oldName", "newName", claims);

        assertThat(claims)
                .doesNotContainKey("newName")
                .containsEntry("other", "value");
    }

    @Test
    void mapClaim_shouldApplyFunctionAndRenameClaim() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("language", "en");

        converter.mapClaim("language", "locale", claims, language -> language.toString().toUpperCase());

        assertThat(claims)
                .containsEntry("locale", "EN")
                .doesNotContainKey("language");
    }

    @Test
    void mapClaim_shouldDoNothingWhenClaimAbsent() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("other", "value");

        converter.mapClaim("language", "locale", claims, value -> value.toString().toUpperCase());

        assertThat(claims)
                .doesNotContainKey("locale")
                .containsEntry("other", "value");
    }

    @Test
    void mapClaim_shouldKeepOriginalClaimNameWhenOriginalNameSameAsMappedName() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("locale", "DE");

        converter.mapClaim("locale", "locale", claims, locale -> "DE");

        assertThat(claims).containsEntry("locale", "DE");
    }

}
