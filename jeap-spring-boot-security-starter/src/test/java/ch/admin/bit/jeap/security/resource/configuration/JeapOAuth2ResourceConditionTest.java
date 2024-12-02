package ch.admin.bit.jeap.security.resource.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JeapOAuth2ResourceConditionTest {

    private static final String AUTH_SERVER_ISSUER_CONFIG_PROPERTY = "jeap.security.oauth2.resourceserver.authorization-server.issuer";
    private static final String B2B_GATEWAY_ISSUER_CONFIG_PROPERTY = "jeap.security.oauth2.resourceserver.b2b-gateway.issuer";
    private static final String FIRST_AUTH_SERVERS_ITEM_ISSUER_CONFIG_PROPERTY = "jeap.security.oauth2.resourceserver.auth-servers[0].issuer";


    @ParameterizedTest
    @ValueSource(strings = {AUTH_SERVER_ISSUER_CONFIG_PROPERTY, B2B_GATEWAY_ISSUER_CONFIG_PROPERTY, FIRST_AUTH_SERVERS_ITEM_ISSUER_CONFIG_PROPERTY})
    void testMatches_IfIssuerPropertyDefined_ThenTrue(final String issuerProperty) {
        final ConditionContext conditionContext = mockConditionContext(Map.of(issuerProperty, "issuer-name"));
        final JeapOAuth2ResourceCondition jeapOAuth2ResourceCondition = new JeapOAuth2ResourceCondition();
        final AnnotatedTypeMetadata annotatedTypeMetadata = Mockito.mock(AnnotatedTypeMetadata.class);

        assertThat(jeapOAuth2ResourceCondition.matches(conditionContext, annotatedTypeMetadata)).isTrue();
    }

    @Test
    void testMatches_IfSeveralIssuerPropertiesDefined_thenTrue() {
        final ConditionContext conditionContext = mockConditionContext(Map.of(
                AUTH_SERVER_ISSUER_CONFIG_PROPERTY, "auth-server-issuer",
                B2B_GATEWAY_ISSUER_CONFIG_PROPERTY, "b2b-issuer",
                FIRST_AUTH_SERVERS_ITEM_ISSUER_CONFIG_PROPERTY, "firs-auth-servers-issuer"));
        final JeapOAuth2ResourceCondition jeapOAuth2ResourceCondition = new JeapOAuth2ResourceCondition();
        final AnnotatedTypeMetadata annotatedTypeMetadata = Mockito.mock(AnnotatedTypeMetadata.class);

        assertThat(jeapOAuth2ResourceCondition.matches(conditionContext, annotatedTypeMetadata)).isTrue();
    }

    @Test
    void testMatches_IfNoIssuerPropertyDefined_thenFalse() {
        final ConditionContext conditionContext = mockConditionContext(Map.of("some-other-property", "some-other-value"));
        final JeapOAuth2ResourceCondition jeapOAuth2ResourceCondition = new JeapOAuth2ResourceCondition();
        final AnnotatedTypeMetadata annotatedTypeMetadata = Mockito.mock(AnnotatedTypeMetadata.class);

        assertThat(jeapOAuth2ResourceCondition.matches(conditionContext, annotatedTypeMetadata)).isFalse();
    }

    private ConditionContext mockConditionContext(Map<String, String> properties) {
        Environment envMock = mock(Environment.class);
        properties.forEach( (key, value) -> when(envMock.getProperty(key)).thenReturn(value));
        ConditionContext contextMock = mock(ConditionContext.class);
        when(contextMock.getEnvironment()).thenReturn(envMock);
        return contextMock;
    }

}
