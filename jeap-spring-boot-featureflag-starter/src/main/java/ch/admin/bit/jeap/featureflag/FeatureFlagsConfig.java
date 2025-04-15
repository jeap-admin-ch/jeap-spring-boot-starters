package ch.admin.bit.jeap.featureflag;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.repository.cache.CachingStateRepository;
import org.togglz.core.repository.file.FileBasedStateRepository;
import org.togglz.core.repository.mem.InMemoryStateRepository;
import org.togglz.spring.boot.actuate.autoconfigure.TogglzAutoConfiguration;
import org.togglz.spring.boot.actuate.autoconfigure.TogglzProperties;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@AutoConfiguration(before=TogglzAutoConfiguration.class)
public class FeatureFlagsConfig {

    private final TogglzProperties togglzProperties;
    private final ResourceLoader resourceLoader;

    public FeatureFlagsConfig(ResourceLoader resourceLoader, TogglzProperties togglzProperties) {
        this.togglzProperties = togglzProperties;
        this.resourceLoader = resourceLoader;
    }

    /**
     * Use the same implementation from TogglzAutoConfiguration$StateRepositoryConfiguration.stateRepository()
     * and add the @RefreshScope annotation.
     */
    @Bean
    @RefreshScope
    @SuppressWarnings("java:S899")
    public StateRepository stateRepository() throws IOException {
        StateRepository stateRepository;
        String featuresFile = togglzProperties.getFeaturesFile();
        if (featuresFile != null) {
            Resource resource = resourceLoader.getResource(featuresFile);
            Integer minCheckInterval = togglzProperties.getFeaturesFileMinCheckInterval();
            File resourceFile = resource.getFile();
            if(togglzProperties.isCreateFeaturesFileIfAbsent() && !resourceFile.exists()) {
                resourceFile.createNewFile();
            }
            if (minCheckInterval != null) {
                stateRepository = new FileBasedStateRepository(resource.getFile(), minCheckInterval);
            } else {
                stateRepository = new FileBasedStateRepository(resource.getFile());
            }
        } else {
            Map<String, TogglzProperties.FeatureSpec> features = togglzProperties.getFeatures();
            stateRepository = new InMemoryStateRepository();
            for (String name : features.keySet()) {
                stateRepository.setFeatureState(features.get(name).state(name));
            }
        }
        // If caching is enabled wrap state repository in caching state repository.
        if (togglzProperties.getCache().isEnabled()) {
            stateRepository = new CachingStateRepository(
                    stateRepository,
                    togglzProperties.getCache().getTimeToLive(),
                    togglzProperties.getCache().getTimeUnit()
            );
        }
        return stateRepository;
    }

}
