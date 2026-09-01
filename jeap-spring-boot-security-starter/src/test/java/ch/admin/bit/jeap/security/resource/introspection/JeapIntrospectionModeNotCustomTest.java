package ch.admin.bit.jeap.security.resource.introspection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JeapIntrospectionModeNotCustomTest {

    private final JeapIntrospectionModeNotCustom condition = new JeapIntrospectionModeNotCustom();

    @Test
    void matches_whenIntrospectionModeNotConfigured_thenTrue() {
        assertThat(condition.matches(conditionContext(new MockEnvironment()), null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"none", "explicit", "ALWAYS", "Lightweight"})
    void matches_whenIntrospectionModeOtherThanCustom_thenTrue(String introspectionMode) {
        assertThat(condition.matches(conditionContext(environmentWithIntrospectionMode(introspectionMode)), null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"custom", "CUSTOM", "Custom"})
    void matches_whenIntrospectionModeCustom_thenFalse(String introspectionMode) {
        assertThat(condition.matches(conditionContext(environmentWithIntrospectionMode(introspectionMode)), null)).isFalse();
    }

    private static MockEnvironment environmentWithIntrospectionMode(String introspectionMode) {
        return new MockEnvironment().withProperty(JeapTokenIntrospectionEnabled.RESOURCE_SERVER_INTROSPECTION_MODE_PROPERTY, introspectionMode);
    }

    private static ConditionContext conditionContext(MockEnvironment environment) {
        ConditionContext conditionContext = mock(ConditionContext.class);
        when(conditionContext.getEnvironment()).thenReturn(environment);
        return conditionContext;
    }

}
