package ch.admin.bit.jeap.config.aws.appconfig.config;

import org.springframework.boot.context.config.*;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class AbstractAppConfigDataLocationResolver<T extends ConfigDataResource> implements ConfigDataLocationResolver<T> {

    protected abstract String getPrefix();

    @Override
    public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
        return location.hasPrefix(getPrefix());
    }

    @Override
    public List<T> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location)
            throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {
        return Collections.emptyList();
    }

    protected List<String> getLocationArgumentStrings(String locationArgsString) {
        if (StringUtils.hasLength(locationArgsString)) {
            return Arrays.asList(locationArgsString.split(";"));
        }
        return Collections.emptyList();
    }
}
