package ch.admin.bit.jeap.security.test.jws;

import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JwsBuilderTest {

    private static final String JWT_ID = "1234567890";
    private static final String ISSUER = "http://localhost/auth";
    private static final String SUBJECT = "69368608-D736-43C8-5F76-55B7BF168299";
    private static final JeapAuthenticationContext CONTEXT = JeapAuthenticationContext.USER;

    private static final String EXT_ID = "287365";
    private static final String ADMIN_DIR_UID = "U11111111";
    private static final String NAME = "Max Muster";
    private static final String FAMILY_NAME = "Muster";
    private static final String GIVEN_NAME = "Max";
    private static final String PREFERRED_USERNAME = "Maximilian";
    private static final String LOCALE = "DE";

    private static final String AUDIENCE_A = "aud a";
    private static final String AUDIENCE_B = "aud b";

    private static final String ROLE_A = "role a";
    private static final String ROLE_B = "role b";
    private static final SemanticApplicationRole ROLE_C = SemanticApplicationRole.builder().
            system("jeap").
            resource("thing").
            operation("oc").
            build();
    private static final SemanticApplicationRole ROLE_D = SemanticApplicationRole.builder().
            system("jeap").
            resource("thing").
            operation("od").
            build();
    private static final String BUSINESS_PARTNER_ID_A = "12345";
    private static final String BUSINESS_PARTNER_ID_B = "67890";
    private static final String BUSINESS_PARTNER_ID_C = "33333";

    private static final String RSA_KEY_STORE_CLASSPATH_RESOURCE_PATH = "/testkeys/default-rsa-test-key-pair.p12";
    private static final String RSA_KEY_STORE_TYPE = "pkcs12";
    private static final String RSA_KEY_ALIAS = "default-test-key";
    private static final String RSA_KEY_PASSWORD = "secret";


    @SuppressWarnings("unchecked")
    @Test
    void testCreateAndBuild() throws Exception {
        final ZonedDateTime issuedAt = ZonedDateTime.now();
        final ZonedDateTime notBefore = issuedAt.minusSeconds(1);
        final ZonedDateTime expiry = notBefore.plusMinutes(3);
        final RSAKey rsaKey = getRsaKey();

        final JwsBuilder builder = JwsBuilder.create(JWT_ID, ISSUER, expiry, notBefore, issuedAt, SUBJECT, CONTEXT);
        final SignedJWT jws = builder.withName(NAME).
                withFamilyName(FAMILY_NAME).
                withGivenName(GIVEN_NAME).
                withPreferredUsername(PREFERRED_USERNAME).
                withExtId(EXT_ID).
                withAdminDirUID(ADMIN_DIR_UID).
                withLocale(LOCALE).
                withUserRoles(ROLE_A, ROLE_B, ROLE_A).
                withUserRoles(ROLE_C, ROLE_D, ROLE_C).
                withBusinessPartnerRoles(BUSINESS_PARTNER_ID_A, ROLE_A, ROLE_B, ROLE_A).
                withBusinessPartnerRoles(BUSINESS_PARTNER_ID_B, ROLE_B).
                withBusinessPartnerRoles(BUSINESS_PARTNER_ID_C, ROLE_C, ROLE_D, ROLE_C).
                withAudiences(AUDIENCE_A, AUDIENCE_B).
                withRsaKey(rsaKey).
                build();
        final String serializedJws = jws.serialize(); // representation used in bearer tokens

        // Check deserialization and signature
        final SignedJWT deserializedJws = SignedJWT.parse(serializedJws);
        final RSAKey rsaPublicJwk = rsaKey.toPublicJWK();
        final JWSVerifier verifier = new RSASSAVerifier(rsaPublicJwk);
        assertThat(deserializedJws.verify(verifier)).isTrue();

        // Check claims
        final JWTClaimsSet claimSet = deserializedJws.getJWTClaimsSet();
        assertThat(claimSet.getJWTID()).isEqualTo(JWT_ID);
        assertThat(claimSet.getIssuer()).isEqualTo(ISSUER);
        assertThat(claimSet.getSubject()).isEqualTo(SUBJECT);
        assertThat(claimSet.getStringClaim(JeapAuthenticationContext.getContextJwtClaimName())).isEqualTo(CONTEXT.name());
        assertThat(claimSet.getStringClaim("ext_id")).isEqualTo(EXT_ID);
        assertThat(claimSet.getStringClaim("admin_dir_uid")).isEqualTo(ADMIN_DIR_UID);
        assertThat(claimSet.getStringClaim("name")).isEqualTo(NAME);
        assertThat(claimSet.getStringClaim("family_name")).isEqualTo(FAMILY_NAME);
        assertThat(claimSet.getStringClaim("given_name")).isEqualTo(GIVEN_NAME);
        assertThat(claimSet.getStringClaim("preferred_username")).isEqualTo(PREFERRED_USERNAME);
        assertThat(claimSet.getStringClaim("locale")).isEqualTo(LOCALE);
        assertThat(claimSet.getStringListClaim("userroles")).containsExactly(ROLE_A, ROLE_B, ROLE_C.toString(), ROLE_D.toString());
        final Map<String, Object> bproles = claimSet.getJSONObjectClaim("bproles");
        assertThat(bproles).hasSize(3);
        assertThat((List<Object>) bproles.get(BUSINESS_PARTNER_ID_A)).containsExactly(ROLE_A, ROLE_B);
        assertThat((List<Object>) bproles.get(BUSINESS_PARTNER_ID_B)).containsExactly(ROLE_B);
        assertThat((List<Object>) bproles.get(BUSINESS_PARTNER_ID_C)).containsExactly(ROLE_C.toString(), ROLE_D.toString());
        assertThat(claimSet.getAudience()).containsExactly(AUDIENCE_A, AUDIENCE_B);
        // date+time values seem to only be stored with seconds precision in the JWT
        assertThat(claimSet.getExpirationTime().toInstant()).isEqualTo(expiry.toInstant().truncatedTo(ChronoUnit.SECONDS));
        assertThat(claimSet.getNotBeforeTime().toInstant()).isEqualTo(notBefore.toInstant().truncatedTo(ChronoUnit.SECONDS));
        assertThat(claimSet.getIssueTime().toInstant()).isEqualTo(issuedAt.toInstant().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void testCreateValidFromNowAndBuild() throws Exception {
        final RSAKey rsaKey = getRsaKey();
        final ZonedDateTime beforeTokenCreation = ZonedDateTime.now();

        final JwsBuilder builder = JwsBuilder.createValidFromNow(SUBJECT, CONTEXT, 3, ChronoUnit.MINUTES);
        final SignedJWT jws = builder.withRsaKey(rsaKey).build();

        final ZonedDateTime afterTokenCreation = ZonedDateTime.now();
        final JWTClaimsSet claimSet = jws.getJWTClaimsSet();
        assertThat(claimSet.getSubject()).isEqualTo(SUBJECT);
        assertThat(claimSet.getStringClaim(JeapAuthenticationContext.getContextJwtClaimName())).isEqualTo(CONTEXT.name());
        assertThat(claimSet.getJWTID()).isEqualTo(JwsBuilder.DEFAULT_JTI);
        assertThat(claimSet.getIssuer()).isEqualTo(JwsBuilder.DEFAULT_ISSUER);
        assertThat(claimSet.getIssueTime()).isEqualTo(claimSet.getNotBeforeTime());
        Instant nbf = claimSet.getNotBeforeTime().toInstant();
        Instant exp =claimSet.getExpirationTime().toInstant();
        assertThat(Duration.between(nbf, exp).equals(Duration.of(3, ChronoUnit.MINUTES))).isTrue();
        assertThat(!nbf.isBefore(beforeTokenCreation.truncatedTo(ChronoUnit.SECONDS).toInstant())).isTrue();
        assertThat(!afterTokenCreation.toInstant().isBefore(nbf)).isTrue();
    }

    @Test
    void testCreateValidForFixedLongPeriodAndBuild() throws Exception {
        final RSAKey rsaKey = getRsaKey();

        final JwsBuilder builder = JwsBuilder.createValidForFixedLongPeriod(SUBJECT, CONTEXT);
        final SignedJWT jws = builder.withRsaKey(rsaKey).build();

        final JWTClaimsSet claimSet = jws.getJWTClaimsSet();
        assertThat(claimSet.getSubject()).isEqualTo(SUBJECT);
        assertThat(claimSet.getStringClaim(JeapAuthenticationContext.getContextJwtClaimName())).isEqualTo(CONTEXT.name());
        assertThat(claimSet.getJWTID()).isEqualTo(JwsBuilder.DEFAULT_JTI);
        assertThat(claimSet.getIssuer()).isEqualTo(JwsBuilder.DEFAULT_ISSUER);
        assertThat(claimSet.getNotBeforeTime().toInstant()).isEqualTo(JwsBuilder.FIXED_PERIOD_NBF.toInstant());
        assertThat(claimSet.getExpirationTime().toInstant()).isEqualTo(JwsBuilder.FIXED_PERIOD_EXP.toInstant());
        assertThat(claimSet.getIssueTime().toInstant()).isEqualTo(JwsBuilder.FIXED_PERIOD_IAT.toInstant());
    }

    @Test
    void testTokenSerializationIsDeterministic() {
        final JwsBuilder builder1 = createValidForFixedLongPeriodBuilder();
        final JwsBuilder builder2 = createValidForFixedLongPeriodBuilder();
        assertThat(builder1.build().serialize()).isEqualTo(builder2.build().serialize());
    }

    private JwsBuilder createValidForFixedLongPeriodBuilder() {
        return JwsBuilder.createValidForFixedLongPeriod(SUBJECT, CONTEXT).
                          withExtId(EXT_ID).
                          withName(NAME).
                          withFamilyName(FAMILY_NAME).
                          withGivenName(GIVEN_NAME).
                          withLocale(LOCALE).
                          withUserRoles(ROLE_A, ROLE_B).
                          withBusinessPartnerRoles(BUSINESS_PARTNER_ID_A, ROLE_A, ROLE_B).
                          withBusinessPartnerRoles(BUSINESS_PARTNER_ID_B, ROLE_B).
                          withAudiences(AUDIENCE_A).
                          withAdminDirUID(ADMIN_DIR_UID).
                          withRsaKey(getRsaKey());
    }


    private RSAKey getRsaKey() {
        return RSAKeyUtils.readRsaKeyPairFromResource(
                new ClassPathResource(RSA_KEY_STORE_CLASSPATH_RESOURCE_PATH), RSA_KEY_STORE_TYPE, RSA_KEY_ALIAS, RSA_KEY_PASSWORD, RSA_KEY_ALIAS);
    }

}
