package ch.admin.bit.jeap.monitor.metrics.truststore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {TrustStoreMetricsAutoConfig.class},
        properties = "jeap.monitor.metrics.truststore.enabled=false")
public class TrustStoreConditionDeactivatedTest {

    @Autowired
    ApplicationContext applicationContext;

    @Test
    void whenRequiredPropertiesAreNotSet_thenExpectMetricsNotToBeInitialized() {
        assertThrows(NoSuchBeanDefinitionException.class, () ->
                applicationContext.getBean(TrustStoreMetricsInitializer.class));
        assertThrows(NoSuchBeanDefinitionException.class, () ->
                applicationContext.getBean(TrustStoreService.class));
    }
}
