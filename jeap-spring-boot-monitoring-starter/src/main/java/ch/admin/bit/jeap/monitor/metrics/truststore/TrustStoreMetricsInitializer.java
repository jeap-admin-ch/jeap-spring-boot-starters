package ch.admin.bit.jeap.monitor.metrics.truststore;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.util.stream.Collectors.toList;


@Slf4j
@RequiredArgsConstructor
// The meter registry api expects generic wildcard types for its arguments
@SuppressWarnings("java:S1452")
class TrustStoreMetricsInitializer {
    private static final String METRIC_NAME = "jeap_trusted_cert";
    private final static DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TrustStoreService trustStoreService;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    void initialize() {
        List<TrustedCert> trustedCerts = trustStoreService.getTrustedCerts();
        List<MultiGauge.Row<?>> rows = rows(trustedCerts);
        MultiGauge.builder(METRIC_NAME)
                .description("Trusted Cert")
                .register(meterRegistry)
                .register(rows);
    }

    private List<MultiGauge.Row<?>> rows(List<TrustedCert> trustedCerts) {
        return trustedCerts.stream()
                .map(TrustStoreMetricsInitializer::row)
                .collect(toList());
    }

    private static MultiGauge.Row<?> row(TrustedCert trustedCert) {
        Tags tags = tags(trustedCert);
        return MultiGauge.Row.of(tags, () -> valueFunction(trustedCert));
    }

    private static Number valueFunction(TrustedCert trustedCert) {
        if (trustedCert.getValidTo() == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), trustedCert.getValidTo());
    }

    private static Tags tags(TrustedCert trustedCert) {
        return Tags.of(
                Tag.of("subject", trustedCert.getSubject()),
                Tag.of("serial", trustedCert.getSerialNumber()),
                Tag.of("from", dateStr(trustedCert.getValidFrom())),
                Tag.of("to", dateStr(trustedCert.getValidTo())));
    }

    private static String dateStr(LocalDate date) {
        if (date == null) {
            return "n/a";
        }
        return DATE_FORMAT.format(date);
    }
}
