package ch.admin.bit.jeap.monitor.metrics.truststore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrustStoreServiceTest extends AbstractTrustStoreTest {

    private TrustStoreService trustStoreService;

    @BeforeEach
    void setUp() {
        trustStoreService = new TrustStoreService();
    }

    @Test
    void getTrustedCerts() {
        List<TrustedCert> trustedCerts = trustStoreService.getTrustedCerts();

        List<TrustedCert> root01 = trustedCerts.stream()
                .filter(c -> c.getSubject().equals("CN=test2.test2,OU=Test2,O=Test2,L=Test2,ST=Test2,C")).toList();

        assertEquals(1, root01.size());
        LocalDate validFrom = root01.getFirst().getValidFrom();
        LocalDate validTo = root01.getFirst().getValidTo();
        assertEquals(LocalDate.of(2024, 11, 13), validFrom);
        assertEquals(LocalDate.of(2025, 11, 13), validTo);
        assertEquals(1, trustedCerts.size());
    }
}
