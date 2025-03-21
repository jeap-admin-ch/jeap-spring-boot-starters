package ch.admin.bit.jeap.monitor.metrics.truststore;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {TrustStoreService.class, TrustStoreMetricsInitializer.class},
        properties = {
                "javax.net.ssl.trustStore=foo",
                "javax.net.ssl.trustStorePassword=bar"})
public class TrustStoreConditionActiveTest {

    @Autowired
    ApplicationContext applicationContext;

    @MockitoBean
    MeterRegistry meterRegistryMock;

    @Test
    void whenRequiredPropertiesAreSet_thenExpectBeansToBeAvailable() {
        assertNotNull(
                applicationContext.getBean(TrustStoreMetricsInitializer.class));
        assertNotNull(
                applicationContext.getBean(TrustStoreService.class));
    }
}
