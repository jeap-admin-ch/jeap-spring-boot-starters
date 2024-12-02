package ch.admin.bit.jeap.monitor.metrics.truststore;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("ConstantConditions")
class TrustStoreMetricsInitializerTest extends AbstractTrustStoreTest {

    private TrustStoreMetricsInitializer trustStoreMetricsInitializer;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        TrustStoreService trustStoreService = new TrustStoreService();
        trustStoreMetricsInitializer = new TrustStoreMetricsInitializer(trustStoreService, meterRegistry);
    }

    @Test
    void initialize() {
        trustStoreMetricsInitializer.initialize();

        Gauge gauge = meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getTag("subject").equals("CN=test2.test2,OU=Test2,O=Test2,L=Test2,ST=Test2,C"))
                .map(meter -> (Gauge) meter)
                .findFirst().orElseThrow();
        long validDays = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.of(2025, 11, 13));
        assertEquals(validDays, gauge.value());
        assertEquals("2024-11-13", gauge.getId().getTag("from"));
        assertEquals("2025-11-13", gauge.getId().getTag("to"));
        assertEquals("475808457958516388995913307891203390944269544165", gauge.getId().getTag("serial"));
    }
}
