package ch.admin.bit.jeap.config.aws.context;

import lombok.experimental.UtilityClass;
import org.springframework.boot.BootstrapRegistry.InstanceSupplier;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;

@UtilityClass
public class ConfigContexts {

    public static <C> void registerAndPromoteBean(ConfigDataLocationResolverContext context, Class<C> type, InstanceSupplier<C> supplier) {
        registerBean(context, type, supplier);
        context.getBootstrapContext().addCloseListener(event -> {
            String name = "configData" + type.getSimpleName();
            if (!event.getApplicationContext().getBeanFactory().containsBean(name)) {
                event.getApplicationContext().getBeanFactory().registerSingleton(name, event.getBootstrapContext().get(type));
            }
        });
    }

    public static <C> void registerBean(ConfigDataLocationResolverContext context, Class<C> type, C instance) {
        context.getBootstrapContext().registerIfAbsent(type, InstanceSupplier.of(instance));
    }

    private static <C> void registerBean(ConfigDataLocationResolverContext context, Class<C> type, InstanceSupplier<C> supplier) {
        ConfigurableBootstrapContext bootstrapContext = context.getBootstrapContext();
        bootstrapContext.registerIfAbsent(type, supplier);
    }
}
