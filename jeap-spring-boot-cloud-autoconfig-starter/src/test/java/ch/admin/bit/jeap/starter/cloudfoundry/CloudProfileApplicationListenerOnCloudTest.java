package ch.admin.bit.jeap.starter.cloudfoundry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.main.cloud-platform=CLOUD_FOUNDRY")
class CloudProfileApplicationListenerOnCloudTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void whenVcapApplicationEnvVarIsPresent_expectCloudProfileToBeActive() {
        Set<String> activeProfiles = Set.of(applicationContext.getEnvironment().getActiveProfiles());
        assertTrue(activeProfiles.contains("cloud"));
    }
}