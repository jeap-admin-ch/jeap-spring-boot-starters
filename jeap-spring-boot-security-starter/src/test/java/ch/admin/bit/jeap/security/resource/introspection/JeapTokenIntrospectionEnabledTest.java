package ch.admin.bit.jeap.security.resource.introspection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JeapTokenIntrospectionEnabledTest {

    private static final String INTROSPECTION_MODE_PROPERTY = "jeap.security.oauth2.resourceserver.introspection.mode";

    private final JeapTokenIntrospectionEnabled condition = new JeapTokenIntrospectionEnabled();

    @Test
    void matches_whenIntrospectionModeNotConfigured_thenFalse() {
        assertThat(condition.matches(conditionContext(new MockEnvironment()), null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"none", "NONE", "None"})
    void matches_whenIntrospectionModeNone_thenFalse(String introspectionMode) {
        assertThat(condition.matches(conditionContext(environmentWithIntrospectionMode(introspectionMode)), null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"explicit", "ALWAYS", "Lightweight", "custom"})
    void matches_whenIntrospectionModeActivatesIntrospection_thenTrue(String introspectionMode) {
        assertThat(condition.matches(conditionContext(environmentWithIntrospectionMode(introspectionMode)), null)).isTrue();
    }

    private static MockEnvironment environmentWithIntrospectionMode(String introspectionMode) {
        return new MockEnvironment().withProperty(INTROSPECTION_MODE_PROPERTY, introspectionMode);
    }

    private static ConditionContext conditionContext(MockEnvironment environment) {
        ConditionContext conditionContext = mock(ConditionContext.class);
        when(conditionContext.getEnvironment()).thenReturn(environment);
        return conditionContext;
    }

}
