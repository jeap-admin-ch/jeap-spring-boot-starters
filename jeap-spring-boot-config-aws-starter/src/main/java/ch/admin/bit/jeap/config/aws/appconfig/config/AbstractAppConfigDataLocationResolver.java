package ch.admin.bit.jeap.config.aws.appconfig.config;

import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.ConfigurableBootstrapContext;
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

    protected <C> void registerAndPromoteBean(ConfigDataLocationResolverContext context, Class<C> type, BootstrapRegistry.InstanceSupplier<C> supplier) {
        registerBean(context, type, supplier);
        context.getBootstrapContext().addCloseListener(event -> {
            String name = "configData" + type.getSimpleName();
            if (!event.getApplicationContext().getBeanFactory().containsBean(name)) {
                event.getApplicationContext().getBeanFactory().registerSingleton(name, event.getBootstrapContext().get(type));
            }
        });
    }

    protected <C> void registerBean(ConfigDataLocationResolverContext context, Class<C> type, C instance) {
        context.getBootstrapContext().registerIfAbsent(type, BootstrapRegistry.InstanceSupplier.of(instance));
    }

    protected <C> void registerBean(ConfigDataLocationResolverContext context, Class<C> type,
                                    BootstrapRegistry.InstanceSupplier<C> supplier) {
        ConfigurableBootstrapContext bootstrapContext = context.getBootstrapContext();
        bootstrapContext.registerIfAbsent(type, supplier);
    }

    protected List<String> getLocationArgumentStrings(String locationArgsString) {
        if (StringUtils.hasLength(locationArgsString)) {
            return Arrays.asList(locationArgsString.split(";"));
        }
        return Collections.emptyList();
    }

}
