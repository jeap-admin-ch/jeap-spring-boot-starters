package ch.admin.bit.jeap.config.aws.appconfig.config;

import org.springframework.boot.context.config.ConfigDataException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

record LocationArgument(String appId, String profileId, boolean optional) {

    private static final Pattern LOCATION_ARGUMENT_PATTERN = Pattern.compile("([^/]+)/([^/]+)");

    public static LocationArgument optional(String appId, String profileId) {
        return new LocationArgument(appId, profileId, true);
    }

    public static LocationArgument mandatory(String appId, String profileId) {
        return new LocationArgument(appId, profileId, false);
    }

    static LocationArgument from(String locationArgumentString) {
        Matcher matcher = LOCATION_ARGUMENT_PATTERN.matcher(locationArgumentString);
        if (!matcher.matches()) {
            throw new ConfigDataInvalidLocationArgumentException(
                    "Invalid location argument: '%s'. Expecting location argument to follow the pattern %s"
                            .formatted(locationArgumentString, LOCATION_ARGUMENT_PATTERN));
        } else {
            return new LocationArgument(matcher.group(1), matcher.group(2), false);
        }
    }

    @Override
    public String toString() {
        return appId + "/" + profileId;
    }

    public static class ConfigDataInvalidLocationArgumentException extends ConfigDataException {
        ConfigDataInvalidLocationArgumentException(String message) {
            super(message, null);
        }
    }

}
