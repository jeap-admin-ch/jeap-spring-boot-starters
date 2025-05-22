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

import static org.assertj.core.api.Assertions.assertThat;

@Import({CustomConditionJeapIntrospectionTest.TestConfig.class})
@ActiveProfiles("introspection-lightweight") // 'lightweight' introspection-mode from profile is overridden with 'custom' below
@EnableConfigurationProperties(ResourceServerProperties.class)
@SpringBootTest(classes = CustomConditionJeapIntrospectionTest.MvcJeapIntrospectionConfigurationUnconditional.class,
                properties = {"jeap.security.resourceserver.introspection.mode=custom"})
class CustomConditionJeapIntrospectionTest {

    private static final String ISSUER_INTROSPECTION_OK = "https://keycloak/auth/realm/introspection-ok";

    @Autowired
    private JeapJwtIntrospectionCondition jwtIntrospectionCondition;

    @Autowired
    private JeapJwtIntrospection jwtIntrospection;

    @Test
    void testCustomConditionActivated() {
        assertThat(jwtIntrospectionCondition).isInstanceOf(TestJeapJwtIntrospectionCondition.class);
    }

    @Test
    void testIntrospectIfNeeded_WhenIntrospectionOK_ThenAdditionalAttributesAdded() {
        Jwt introspectedJwt = jwtIntrospection.introspectIfNeeded(createJwt(ISSUER_INTROSPECTION_OK));
        assertThat(introspectedJwt.getClaimAsString("iss")).isEqualTo(ISSUER_INTROSPECTION_OK);
        assertThat(introspectedJwt.getClaimAsBoolean("active")).isTrue();
        assertThat(introspectedJwt.getClaimAsBoolean("introspected")).isTrue();
    }

    private Jwt createJwt(String issuer) {
        return Jwt.withTokenValue("dummy")
                .header("dummy-header", "dummy-value") // at least one header required
                .issuer(issuer)
                .build();
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
        // Provide a custom JeapJwtIntrospectionCondition that always returns true
        JeapJwtIntrospectionCondition jeapJwtIntrospectionCondition() {
            return new TestJeapJwtIntrospectionCondition();
        }

        @Bean
        // Provide a custom JeapTokenIntrospectorFactory that returns an introspected=true attribute
        JeapTokenIntrospectorFactory jeapTokenIntrospectorFactory() {
            return new IntrospectedTrueJeapTokenIntrospectorFactory();
        }
    }

    private static class TestJeapJwtIntrospectionCondition implements JeapJwtIntrospectionCondition {
        @Override
        public boolean needsIntrospection(Jwt jwt) {
            return true;
        }
    }

}
