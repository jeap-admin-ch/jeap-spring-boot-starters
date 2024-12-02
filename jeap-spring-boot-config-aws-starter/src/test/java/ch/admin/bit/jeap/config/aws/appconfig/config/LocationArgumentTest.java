package ch.admin.bit.jeap.config.aws.appconfig.config;

import ch.admin.bit.jeap.config.aws.appconfig.config.LocationArgument.ConfigDataInvalidLocationArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocationArgumentTest {

    @Test
    void testFromString_validLocationArgumentString() {
        LocationArgument locationArgument = LocationArgument.from("app/profile");
        assertThat(locationArgument.appId()).isEqualTo("app");
        assertThat(locationArgument.profileId()).isEqualTo("profile");
    }


    @ParameterizedTest
    @ValueSource(strings={"", "app", "app/", "app/profile/", "app/profile/whatever"})
    void testFromString_invalidLocationArgumentString(String invalidLocationArgumentString) {
        assertThatThrownBy(() -> LocationArgument.from(invalidLocationArgumentString))
                .isInstanceOf(ConfigDataInvalidLocationArgumentException.class)
                .hasMessageStartingWith("Invalid location argument");
    }

}
