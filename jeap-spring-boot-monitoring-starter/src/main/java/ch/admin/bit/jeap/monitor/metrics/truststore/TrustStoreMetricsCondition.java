package ch.admin.bit.jeap.monitor.metrics.truststore;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ConfigurationCondition;

/**
 * The trust store metrics are activated if java.net.ssl properties for the truststore are set,
 * and jeap.monitor.metrics.truststore.enabled is either "true" or not set.
 */
public class TrustStoreMetricsCondition extends AllNestedConditions {

    TrustStoreMetricsCondition() {
        super(ConfigurationCondition.ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(TrustStoreService.JAVAX_NET_SSL_TRUST_STORE)
    static class OnTrustStoreProperty {
    }

    @ConditionalOnProperty(TrustStoreService.JAVAX_NET_SSL_TRUST_STORE_PASSWORD)
    static class OnTrustStorePasswordProperty {
    }

    @ConditionalOnProperty(name = "jeap.monitor.metrics.truststore.enabled", matchIfMissing = true, havingValue = "true")
    static class OnTrustStoreMetrics {
    }
}
