package ch.admin.bit.jeap.config.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.*;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.boot.util.Instantiator;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.ContextRefreshedWithApplicationEvent;
import org.springframework.cloud.context.refresh.ConfigDataContextRefresher;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.List;
import java.util.function.Supplier;

/**
 * This class fixes the updateEnvironment method from ConfigDataContextRefresher in order to detect config property
 * changes caused by the removal of config sources in the config server. The code of the method is duplicated from
 * ConfigDataContextRefresher and a small additional code part is added that removes config server property sources from the
 * client context if there is no longer a matching config server property source.
 *
 * @see FixedLegacyContextRefresher
 */
class FixedConfigDataContextRefresher extends ConfigDataContextRefresher {
    private static final String CONFIGSERVER = "configserver:";
    private static final String OPTIONAL_CONFIGSERVER = "optional:" + CONFIGSERVER;

    private SpringApplication application;

    public FixedConfigDataContextRefresher(ConfigurableApplicationContext context, RefreshScope scope, RefreshAutoConfiguration.RefreshProperties properties) {
        super(context, scope, properties);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedWithApplicationEvent event) {
        super.onApplicationEvent(event);
        application = event.getSpringApplication();
    }

    @Override
    protected void updateEnvironment() {
        if (logger.isTraceEnabled()) {
            logger.trace("Re-processing environment to add config data");
        }
        StandardEnvironment environment = copyEnvironment(getContext().getEnvironment());
        ConfigurableBootstrapContext bootstrapContext = getContext().getBeanProvider(ConfigurableBootstrapContext.class)
                .getIfAvailable(DefaultBootstrapContext::new);

        // run thru all EnvironmentPostProcessor instances. This lets things like vcap and
        // decrypt happen after refresh. The hard coded call to
        // ConfigDataEnvironmentPostProcessor.applyTo() is now automated as well.
        DeferredLogFactory logFactory = new PassthruDeferredLogFactory();
        List<String> classNames = SpringFactoriesLoader.loadFactoryNames(EnvironmentPostProcessor.class,
                getClass().getClassLoader());
        Instantiator<EnvironmentPostProcessor> instantiator = new Instantiator<>(EnvironmentPostProcessor.class,
                (parameters) -> {
                    parameters.add(DeferredLogFactory.class, logFactory);
                    parameters.add(Log.class, logFactory::getLog);
                    parameters.add(ConfigurableBootstrapContext.class, bootstrapContext);
                    parameters.add(BootstrapContext.class, bootstrapContext);
                    parameters.add(BootstrapRegistry.class, bootstrapContext);
                });
        List<EnvironmentPostProcessor> postProcessors = instantiator.instantiate(classNames);
        for (EnvironmentPostProcessor postProcessor : postProcessors) {
            postProcessor.postProcessEnvironment(environment, application);
        }

        if (environment.getPropertySources().contains(REFRESH_ARGS_PROPERTY_SOURCE)) {
            environment.getPropertySources().remove(REFRESH_ARGS_PROPERTY_SOURCE);
        }
        MutablePropertySources target = getContext().getEnvironment().getPropertySources();
        String targetName = null;
        for (PropertySource<?> source : environment.getPropertySources()) {
            String name = source.getName();
            if (target.contains(name)) {
                targetName = name;
            }
            if (!this.standardSources.contains(name)) {
                if (target.contains(name)) {
                    target.replace(name, source);
                } else {
                    if (targetName != null) {
                        target.addAfter(targetName, source);
                        // update targetName to preserve ordering
                        targetName = name;
                    } else {
                        // targetName was null so we are at the start of the list
                        target.addFirst(source);
                        targetName = name;
                    }
                }
            }
        }
        // This is the additional code for the fix
        MutablePropertySources originPropertySources = environment.getPropertySources();
        for (PropertySource<?> targetPropertySource : target) {
            String targetPropertySourceName = targetPropertySource.getName();
            if (isConfigserverPropertySource(targetPropertySourceName) &&
                    !originPropertySources.contains(targetPropertySourceName)) {
                // the target property source from the config server no longer has a matching property source in config server -> remove it
                target.remove(targetPropertySourceName);
            }
        }
        // end additional code
    }

    private static boolean isConfigserverPropertySource(String targetPropertySourceName) {
        return targetPropertySourceName.startsWith(CONFIGSERVER) ||
                targetPropertySourceName.startsWith(OPTIONAL_CONFIGSERVER);
    }

    static class PassthruDeferredLogFactory implements DeferredLogFactory {

        @Override
        public Log getLog(Supplier<Log> destination) {
            return destination.get();
        }

        @Override
        public Log getLog(Class<?> destination) {
            return getLog(() -> LogFactory.getLog(destination));
        }

        @Override
        public Log getLog(Log destination) {
            return getLog(() -> destination);
        }

    }
}