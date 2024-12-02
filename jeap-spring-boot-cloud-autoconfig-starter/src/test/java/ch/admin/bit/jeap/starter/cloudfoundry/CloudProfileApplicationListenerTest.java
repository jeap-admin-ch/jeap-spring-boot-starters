package ch.admin.bit.jeap.starter.cloudfoundry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
class CloudProfileApplicationListenerTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void whenVcapApplicationEnvVarIsNotPresent_expectCloudProfileNotToBeActive() {
        Set<String> activeProfiles = Set.of(applicationContext.getEnvironment().getActiveProfiles());
        assertFalse(activeProfiles.contains("cloud"));
    }
}