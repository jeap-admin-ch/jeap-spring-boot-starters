package ch.admin.bit.jeap.security.resource.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class IntrospectionModeTest {

    @ParameterizedTest
    @EnumSource(value = IntrospectionMode.class, names = {"NONE"}, mode = EnumSource.Mode.EXCLUDE)
    void doesActivateIntrospection_whenModeOtherThanNone_thenTrue(IntrospectionMode mode) {
        assertThat(mode.doesActivateIntrospection()).isTrue();
    }

    @Test
    void doesActivateIntrospection_whenModeNone_thenFalse() {
        assertThat(IntrospectionMode.NONE.doesActivateIntrospection()).isFalse();
    }

}
