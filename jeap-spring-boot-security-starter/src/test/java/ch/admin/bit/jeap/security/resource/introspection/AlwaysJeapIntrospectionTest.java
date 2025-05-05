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

@Import({AlwaysJeapIntrospectionTest.TestConfig.class})
@ActiveProfiles("introspection-lightweight") // 'lightweight' introspection-mode from profile is overridden with 'always' below
@EnableConfigurationProperties(ResourceServerProperties.class)
@SpringBootTest(classes = AlwaysJeapIntrospectionTest.MvcJeapIntrospectionConfigurationUnconditional.class,
                properties = {"jeap.security.oauth2.resourceserver.introspection-mode=always"})
class AlwaysJeapIntrospectionTest {

    private static final String ISSUER_INTROSPECTION_OK = "https://keycloak/auth/realm/introspection-ok";

    @Autowired
    private JeapJwtIntrospectionCondition jwtIntrospectionCondition;

    @Autowired
    private JeapJwtIntrospection jwtIntrospection;

    @Test
    void testAlwaysTokenIntrospectionConditionActivated() {
        assertThat(jwtIntrospectionCondition).isInstanceOf(AlwaysTokenIntrospectionCondition.class);
    }

    @Test
    void testIntrospectIfNeededDoesIntrospect() {
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
        JeapTokenIntrospectorFactory jeapTokenIntrospectorFactory() {
            return new IntrospectedTrueJeapTokenIntrospectorFactory();
        }
    }

}
