package ch.admin.bit.jeap.security.resource.introspection;

import ch.admin.bit.jeap.security.resource.properties.ResourceServerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static ch.admin.bit.jeap.security.resource.introspection.LightweightTokenIntrospectionCondition.ROLES_PRUNED_CHARS_CLAIM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({LightweightJeapIntrospectionTest.TestConfig.class})
@ActiveProfiles("introspection-lightweight")
@EnableConfigurationProperties(ResourceServerProperties.class)
@SpringBootTest(classes = LightweightJeapIntrospectionTest.MvcJeapIntrospectionConfigurationUnconditional.class)
class LightweightJeapIntrospectionTest {

    private static final String ISSUER_INTROSPECTION_TOKEN_NOT_ACTIVE = "https://b2b/auth/introspection-token-not-active";
    private static final String ISSUER_INTROSPECTION_EXCEPTION = "https://custom-uri/auth/realm/introspection-exception";
    private static final String ISSUER_INTROSPECTION_OTHER_EXCEPTION = "https://custom-uri/auth/realm/introspection-other-exception";
    private static final String ISSUER_INTROSPECTION_OK = "https://keycloak/auth/realm/introspection-ok";

    @Autowired
    private JeapJwtIntrospection jwtIntrospection;

    @Test
    void testIsValid() {
        assertThat(jwtIntrospection.isValid(createLightweightJwt(ISSUER_INTROSPECTION_OK))).isTrue();
        assertThat(jwtIntrospection.isValid(createLightweightJwt(ISSUER_INTROSPECTION_TOKEN_NOT_ACTIVE))).isFalse();
        assertThat(jwtIntrospection.isValid(createLightweightJwt(ISSUER_INTROSPECTION_EXCEPTION))).isFalse();
        assertThat(jwtIntrospection.isValid(createLightweightJwt(ISSUER_INTROSPECTION_OTHER_EXCEPTION))).isFalse();
    }

    @Test
    void testIntrospectIfNeeded_WhenIntrospectionOK_ThenAdditionalAttributesAdded() {
        Jwt introspectedJwt = jwtIntrospection.introspectIfNeeded(createLightweightJwt(ISSUER_INTROSPECTION_OK));
        assertThat(introspectedJwt.getClaimAsString("iss")).isEqualTo(ISSUER_INTROSPECTION_OK);
        assertThat(introspectedJwt.getSubject()).isEqualTo("1234567890");
        assertThat(introspectedJwt.getClaimAsBoolean("active")).isTrue();
        assertThat(introspectedJwt.getClaimAsString("additional-claim-1")).isEqualTo("additional-value-1");
        assertThat(introspectedJwt.getClaimAsString("additional-claim-2")).isEqualTo("additional-value-2");
    }

    @Test
    void testIntrospectIfNeeded_WhenIntrospectionOK_ThenAdditionalAttributesAddedButExistingNotOverwritten() {
        Jwt introspectedJwt = jwtIntrospection.introspectIfNeeded(createJwt(
                // Just for testing purposes. Introspection should not return attribute values that don't match the
                // corresponding claim values in the introspected token.
                ISSUER_INTROSPECTION_OK, true, Map.of("additional-claim-1", "existing-value-1")));
        assertThat(introspectedJwt.getClaimAsString("iss")).isEqualTo(ISSUER_INTROSPECTION_OK);
        assertThat(introspectedJwt.getSubject()).isEqualTo("1234567890");
        assertThat(introspectedJwt.getClaimAsBoolean("active")).isTrue();
        assertThat(introspectedJwt.getClaimAsString("additional-claim-1")).isEqualTo("existing-value-1");
        assertThat(introspectedJwt.getClaimAsString("additional-claim-2")).isEqualTo("additional-value-2");
    }

    @Test
    void testIntrospectIfNeeded_WhenIntrospectionNotNeeded_ThenNoAdditionalAttributesAdded() {
        Jwt introspectedJwt = jwtIntrospection.introspectIfNeeded(createJwt(ISSUER_INTROSPECTION_OK, false));
        assertThat(introspectedJwt.getClaimAsString("iss")).isEqualTo(ISSUER_INTROSPECTION_OK);
        assertThat(introspectedJwt.getSubject()).isEqualTo("1234567890");
        assertThat(introspectedJwt.getClaimAsBoolean("active")).isNull();
        assertThat(introspectedJwt.getClaimAsString("additional-claim-1")).isNull();
        assertThat(introspectedJwt.getClaimAsString("additional-claim-2")).isNull();
    }

    @Test
    void testIntrospectIfNeeded_WhenTokenInvalid_ThenThrowsJeapIntrospectionInvalidTokenException() {
        final Jwt jwtInvalidTokenException = createLightweightJwt(ISSUER_INTROSPECTION_TOKEN_NOT_ACTIVE);
        assertThatThrownBy(() -> jwtIntrospection.introspectIfNeeded(jwtInvalidTokenException)).
                isInstanceOf(JeapIntrospectionInvalidTokenException.class);
    }

    @Test
    void testIntrospectIfNeeded_WhenIntrospectionException_ThenThrowsJeapIntrospectionException() {
        final Jwt jwtIntrospectionException = createLightweightJwt(ISSUER_INTROSPECTION_EXCEPTION);
        assertThatThrownBy(() -> jwtIntrospection.introspectIfNeeded(jwtIntrospectionException)).
                isInstanceOf(JeapIntrospectionException.class);
    }

    @Test
    void testIntrospectIfNeeded_WhenOtherException_ThenThrowsJeapIntrospectionException() {
        final Jwt jwtOtherException = createLightweightJwt(ISSUER_INTROSPECTION_OTHER_EXCEPTION);
        assertThatThrownBy(() -> jwtIntrospection.introspectIfNeeded(jwtOtherException)).
                isInstanceOf(JeapIntrospectionException.class);
    }

    private Jwt createLightweightJwt(String issuer) {
        return createJwt(issuer, true);
    }

    private Jwt createJwt(String issuer, boolean lightweight) {
        return createJwt(issuer, lightweight, null);
    }

    private Jwt createJwt(String issuer, boolean lightweight, Map<String, Object> additionalClaims) {
        var jwtBuilder = Jwt.withTokenValue("dummy")
                .header("dummy-header", "dummy-value") // at least one header required
                .issuer(issuer)
                .subject("1234567890");
        if (lightweight) {
            jwtBuilder.claim(ROLES_PRUNED_CHARS_CLAIM, 10000);
        }
        if (additionalClaims != null) {
            additionalClaims.forEach(jwtBuilder::claim);
        }
        return jwtBuilder.build();
    }

    @Configuration
    // We need to get rid of the "@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)" on
    // MvcJeapIntrospectionConfiguration to be able to activate the configuration class in this test. As this project
    // provides both the WebMVC and the WebFlux dependencies, the conditional-on-servlet-webapp would not be satisfied
    // otherwise.
    static class MvcJeapIntrospectionConfigurationUnconditional extends MvcJeapIntrospectionConfiguration {}

    @TestConfiguration
    static class TestConfig {
        @Bean
        // Provide a custom JeapTokenIntrospectorFactory to simulate different introspection results
        JeapTokenIntrospectorFactory jeapTokenIntrospectorFactory() {
            return new TestJeapTokenIntrospectorFactory();
        }
    }

    private static class TestJeapTokenIntrospectorFactory implements JeapTokenIntrospectorFactory {
        @Override
        public JeapTokenIntrospector create(JeapTokenIntrospectorConfiguration config) {
            return switch (config.issuer()) {
                case ISSUER_INTROSPECTION_TOKEN_NOT_ACTIVE -> TestJeapTokenIntrospectorFactory::introspectInvalidTokenException;
                case ISSUER_INTROSPECTION_EXCEPTION -> TestJeapTokenIntrospectorFactory::introspectIntrospectionException;
                case ISSUER_INTROSPECTION_OTHER_EXCEPTION -> TestJeapTokenIntrospectorFactory::introspectOtherException;
                case ISSUER_INTROSPECTION_OK -> TestJeapTokenIntrospectorFactory::introspectAttributes;
                default -> throw new IllegalArgumentException("Unknown issuer: " + config.issuer());
            };
        }

        private static Map<String, Object> introspectInvalidTokenException(String token) {
            throw new JeapIntrospectionInvalidTokenException();
        }

        private static Map<String, Object> introspectIntrospectionException(String token) {
            throw new JeapIntrospectionException("some introspection exception");
        }

        private static Map<String, Object> introspectOtherException(String token) {
            throw new IllegalStateException("some other exception");
        }

        private static Map<String, Object> introspectAttributes(String token) {
            return Map.of(
                    "sub", "1234567890",
                    "active", true,
                    "additional-claim-1", "additional-value-1",
                    "additional-claim-2", "additional-value-2");
        }
    }

}
