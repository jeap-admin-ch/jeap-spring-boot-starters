package ch.admin.bit.jeap.featureflag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext // test sets a system property
class FeatureFlagsTest {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private ContextRefresher refresher;

    @Test
    void testFeatureFlags() {
        // Asserting the initial feature flag configuration
        assertThat(FeatureFlags.MY_FIRST_FEATURE_FLAG.isActive()).isTrue();
        assertThat(FeatureFlags.MY_SECOND_FEATURE_FLAG.isActive()).isFalse();

        // Changing first feature flag configuration to false and refreshing the application context
        System.setProperty("togglz.features.MY_FIRST_FEATURE_FLAG.enabled", "false");
        refresher.refresh();

        // Asserting that the first feature flag configuration has changed
        assertThat(FeatureFlags.MY_FIRST_FEATURE_FLAG.isActive()).isFalse();
        assertThat(FeatureFlags.MY_SECOND_FEATURE_FLAG.isActive()).isFalse();
        System.clearProperty("togglz.features.MY_FIRST_FEATURE_FLAG.enabled");
    }

}