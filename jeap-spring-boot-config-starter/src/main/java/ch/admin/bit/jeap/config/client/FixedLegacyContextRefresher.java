package ch.admin.bit.jeap.config.client;


import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.bootstrap.BootstrapApplicationListener;
import org.springframework.cloud.bootstrap.BootstrapConfigFileApplicationListener;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.context.refresh.LegacyContextRefresher;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.cloud.bootstrap.config.PropertySourceBootstrapConfiguration.BOOTSTRAP_PROPERTY_SOURCE_NAME;

/**
 * This class fixes the addConfigFilesToEnvironment method from LegacyContextRefresher in order to detect config property
 * changes caused by the removal of config sources in the config sever. The code of the method is duplicated from
 * LegacyContextRefresher and a small additional code part is added that removes config server property sources from the
 * client context if there is no longer a matching config server property source.
 *
 * @see FixedConfigDataContextRefresher
 */
public class FixedLegacyContextRefresher extends LegacyContextRefresher {

    public FixedLegacyContextRefresher(ConfigurableApplicationContext context, RefreshScope scope,
                                       RefreshAutoConfiguration.RefreshProperties properties) {
        super(context, scope, properties);
    }

    @Override
    protected void updateEnvironment() {
        fixedAddConfigFilesToEnvironment();
    }

    /* For testing. */ ConfigurableApplicationContext fixedAddConfigFilesToEnvironment() {
        ConfigurableApplicationContext capture = null;

        try {
            StandardEnvironment environment = this.copyEnvironment(this.getContext().getEnvironment());
            Map<String, Object> map = new HashMap();
            map.put("spring.jmx.enabled", false);
            map.put("spring.main.sources", "");
            map.put("spring.main.web-application-type", "NONE");
            map.put("spring.cloud.bootstrap.enabled", Boolean.TRUE.toString());
            environment.getPropertySources().addFirst(new MapPropertySource("refreshArgs", map));
            SpringApplicationBuilder builder = (new SpringApplicationBuilder(new Class[]{ContextRefresher.Empty.class})).bannerMode(Banner.Mode.OFF).web(WebApplicationType.NONE).environment(environment);
            builder.application().setListeners(Arrays.asList(new BootstrapApplicationListener(), new BootstrapConfigFileApplicationListener()));
            capture = builder.run(new String[0]);
            if (environment.getPropertySources().contains("refreshArgs")) {
                environment.getPropertySources().remove("refreshArgs");
            }

            MutablePropertySources target = this.getContext().getEnvironment().getPropertySources();
            String targetName = null;

            for (PropertySource<?> source : environment.getPropertySources()) {
                String name = source.getName();
                if (target.contains(name)) {
                    targetName = name;
                }

                if (!this.standardSources.contains(name)) {
                    if (target.contains(name)) {
                        target.replace(name, source);
                    } else if (targetName != null) {
                        target.addAfter(targetName, source);
                        targetName = name;
                    } else {
                        target.addFirst(source);
                        targetName = name;
                    }
                }
            }
            // This is the additional code for the fix
            MutablePropertySources originPropertySources = environment.getPropertySources();
            for (PropertySource<?> targetPropertySource : target) {
                String targetPropertySourceName = targetPropertySource.getName();
                if (targetPropertySourceName.startsWith(BOOTSTRAP_PROPERTY_SOURCE_NAME + "-") &&
                        !originPropertySources.contains(targetPropertySourceName)) {
                    // the target property source from the config server no longer has a matching property source in config server -> remove it
                    target.remove(targetPropertySourceName);
                }
            }
            // end additional code
        } finally {
            for (ConfigurableApplicationContext closeable = capture; closeable != null; closeable = (ConfigurableApplicationContext) closeable.getParent()) {
                try {
                    closeable.close();
                } catch (Exception var16) {
                }

                if (!(closeable.getParent() instanceof ConfigurableApplicationContext)) {
                    break;
                }
            }
        }
        return capture;
    }

}
