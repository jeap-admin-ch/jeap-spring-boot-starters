package ch.admin.bit.jeap.monitor.metrics.truststore;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.Objects;

public abstract class AbstractTrustStoreTest {
    private static String previousTrustStorePropertyValue;
    private static String previousTrustStorePasswordValue;

    @BeforeAll
    static void setTrustStoreProperties() {
        previousTrustStorePropertyValue = System.getProperty(TrustStoreService.JAVAX_NET_SSL_TRUST_STORE);
        previousTrustStorePasswordValue = System.getProperty(TrustStoreService.JAVAX_NET_SSL_TRUST_STORE_PASSWORD);
        String filePath = Objects.requireNonNull(Thread.currentThread()
                .getContextClassLoader().getResource("test-truststore.jks")).getFile();
        System.setProperty(TrustStoreService.JAVAX_NET_SSL_TRUST_STORE, filePath);
        System.setProperty(TrustStoreService.JAVAX_NET_SSL_TRUST_STORE_PASSWORD, "changeit");
    }

    @AfterAll
    static void resetTrustStoreProperties() {
        if (previousTrustStorePropertyValue == null) {
            System.clearProperty(TrustStoreService.JAVAX_NET_SSL_TRUST_STORE);
        } else {
            System.setProperty(TrustStoreService.JAVAX_NET_SSL_TRUST_STORE, previousTrustStorePropertyValue);
        }
        if (previousTrustStorePasswordValue == null) {
            System.clearProperty(TrustStoreService.JAVAX_NET_SSL_TRUST_STORE_PASSWORD);
        } else {
            System.setProperty(TrustStoreService.JAVAX_NET_SSL_TRUST_STORE_PASSWORD, previousTrustStorePasswordValue);
        }
    }
}
