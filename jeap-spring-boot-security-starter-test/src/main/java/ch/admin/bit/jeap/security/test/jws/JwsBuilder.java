package ch.admin.bit.jeap.security.test.jws;

import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;

import static ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext.B2B;
import static java.util.Arrays.asList;

// @jEAP team: This builder does not follow the same conventions as the Lombok builders usually used by jEAP.
// -> when breaking changes are introduced, adapt this builder to the Lombok style. See also JeapAuthenticationTestTokenBuilder.
/**
 * Builder for creating signed Java web tokens (JWS). Provides custom builder methods for the most important claims and
 * a generic builder method withClaim() to add arbitrary claims.
 */
public class JwsBuilder {

    public static final String DEFAULT_JTI = "test-token";
    public static final String DEFAULT_ISSUER = "http://localhost/auth";
    public static final String B2B_ISSUER = "http://localhost/b2b/auth";

    public static final ZonedDateTime FIXED_PERIOD_IAT = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC+1"));
    public static final ZonedDateTime FIXED_PERIOD_NBF = FIXED_PERIOD_IAT;
    public static final ZonedDateTime FIXED_PERIOD_EXP = FIXED_PERIOD_NBF.plusYears(100);

    private static final String USERROLES_CLAIM_NAME = "userroles";
    private static final String BPROLES_CLAIM_NAME = "bproles";

    private final JWTClaimsSet.Builder jwtClaimSetBuilder = new JWTClaimsSet.Builder();
    private final Set<String> audiences = new LinkedHashSet<>();
    private final Set<String> userRoles = new LinkedHashSet<>();
    private final Map<String, Set<String>> businessPartnerRoles = new HashMap<>();
    private RSAKey rsaKey;

    /**
     * Create a builder instance with the given mandatory claim values.
     */
    public static JwsBuilder create(String jwtId, String issuer, ZonedDateTime expiry, ZonedDateTime notBefore, ZonedDateTime issuedAt, String subject, JeapAuthenticationContext context) {
        return new JwsBuilder(jwtId, issuer, expiry, notBefore, issuedAt, subject, context);
    }

    /**
     * Create a builder instance that creates a JWS valid for a defined duration of time starting from now.
     * The jti claim (JWT id) and the iss claim (issuer) are populated with default values.
     *
     * @param subject The token's subject.
     * @param context The token's authentication context.
     * @param validity The number of temporal units the created token should be valid starting from now.
     * @param temporalUnit The temporal unit of the validity.
     * @return The builder instance.
     */
    public static JwsBuilder createValidFromNow(String subject, JeapAuthenticationContext context, long validity, TemporalUnit temporalUnit) {
        ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS); // The JWS precision seems to be seconds only.
        ZonedDateTime expiry = now.plus(Duration.of(validity, temporalUnit));
        return create(DEFAULT_JTI, getIssuerForContext(context), expiry, now, now, subject, context);
    }

    /**
     * Create a builder instance that creates a JWS valid for the fixed long time period defined by DEFAULT_NBF and DEFAULT_EXP.
     * The iat claim (issued at) is set to the same value as the nbf claim. The jti (JWT id) and iss (issuer) claims are
     * populated with default values. Tokens created by this builder can be useful for tests that don't want the token's
     * serialized value to change with every test run (e.g. Pact tests).
     *
     * @param subject The token's subject.
     * @param context The token's authentication context.
     * @return The builder instance.
     */
    public static JwsBuilder createValidForFixedLongPeriod(String subject, JeapAuthenticationContext context) {
        return create(DEFAULT_JTI, getIssuerForContext(context), FIXED_PERIOD_EXP, FIXED_PERIOD_NBF, FIXED_PERIOD_IAT, subject, context);
    }

    public static String getIssuerForContext(JeapAuthenticationContext jeapAuthenticationContext)  {
        return jeapAuthenticationContext != B2B ? DEFAULT_ISSUER : B2B_ISSUER;
    }

    private JwsBuilder(String jwtId, String issuer, ZonedDateTime expiry, ZonedDateTime notBefore, ZonedDateTime issuedAt, String subject, JeapAuthenticationContext context) {
        jwtClaimSetBuilder.
                jwtID(jwtId).
                issuer(issuer).
                expirationTime(Date.from(expiry.toInstant())).
                notBeforeTime(Date.from(notBefore.toInstant())).
                issueTime(Date.from(issuedAt.toInstant())).
                subject(subject).
                claim(JeapAuthenticationContext.getContextJwtClaimName(), context.name());
    }

    public SignedJWT build() {
        if (!audiences.isEmpty()) {
            jwtClaimSetBuilder.audience(new ArrayList<>(audiences));
        }
        if (!userRoles.isEmpty()) {
            jwtClaimSetBuilder.claim(USERROLES_CLAIM_NAME, new ArrayList<>(userRoles));
        }
        if (!businessPartnerRoles.isEmpty()) {
            jwtClaimSetBuilder.claim(BPROLES_CLAIM_NAME, businessPartnerRoles);
        }

        return createJws(jwtClaimSetBuilder.build());
    }

    public JwsBuilder withIssuer(String issuer) {
        jwtClaimSetBuilder.issuer(issuer);
        return this;
    }

    public JwsBuilder withAudiences(String... audiences) {
        this.audiences.addAll(asList(audiences));
        return this;
    }

    public JwsBuilder withExtId(String extId) {
        return withClaim("ext_id", extId);
    }

    public JwsBuilder withAdminDirUID(String adminDirUID) {
        return withClaim("admin_dir_uid", adminDirUID);
    }

    public JwsBuilder withName(String name) {
        return withClaim("name", name);
    }

    public JwsBuilder withGivenName(String givenName) {
        return withClaim("given_name", givenName);
    }

    public JwsBuilder withFamilyName(String familyName) {
        return withClaim("family_name", familyName);
    }

    public JwsBuilder withPreferredUsername(String preferredUsername) {
        return withClaim("preferred_username", preferredUsername);
    }

    public JwsBuilder withLocale(String locale) {
        return withClaim("locale", locale);
    }

    /**
     * Set the specified claim.
     *
     * @param claimName The name of the claim to set.
     * @param claimValue The value of the claim to set. Should map to a JSON entity.
     * @return The builder.
     */
    public JwsBuilder withClaim(String claimName, Object claimValue) {
        jwtClaimSetBuilder.claim(claimName, claimValue);
        return this;
    }

    public JwsBuilder withUserRoles(String... roles) {
        userRoles.addAll(asList(roles));
        return this;
    }

    public JwsBuilder withUserRoles(SemanticApplicationRole... roles) {
        Arrays.stream(roles)
            .map(SemanticApplicationRole::toString)
            .forEach(userRoles::add);
        return this;
    }

    public JwsBuilder withBusinessPartnerRoles(String businessPartner, String... roles) {
        Set<String> currentRoles = businessPartnerRoles.computeIfAbsent(businessPartner, k -> new HashSet<>());
        currentRoles.addAll(asList(roles));
        return this;
    }

    public JwsBuilder withBusinessPartnerRoles(String businessPartner, SemanticApplicationRole... roles) {
        Set<String> currentRoles = businessPartnerRoles.computeIfAbsent(businessPartner, k -> new HashSet<>());
        Arrays.stream(roles)
                .map(SemanticApplicationRole::toString)
                .forEach(currentRoles::add);
        return this;
    }

    /**
     * Provide the RSA key used to sign the token. If no key is provided a newly created random key will be used to sign the token.
     *
     * @param rsaKey The RSA key used to sign the token.
     * @return The builder.
     */
    public JwsBuilder withRsaKey(RSAKey rsaKey)  {
        this.rsaKey = rsaKey;
        return this;
    }

    private SignedJWT createJws(JWTClaimsSet jwtClaimsSet) {
        try {
            RSAKey jwkRsaKey = getRsaKey();
            JWSSigner signer = new RSASSASigner(jwkRsaKey);
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS512).keyID(jwkRsaKey.getKeyID()).build(), jwtClaimsSet);
            signedJWT.sign(signer);
            return signedJWT;
        } catch (JOSEException e) {
            throw new IllegalStateException("An unexpected JOSE exception ocurred", e);
        }
    }

    private RSAKey getRsaKey()  {
        if (rsaKey != null)  {
            return rsaKey;
        }
        else {
            return RSAKeyUtils.createRsaKeyPair();
        }
    }

}
