package ch.admin.bit.jeap.monitor.metrics.truststore;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

@Value
@Builder
@Slf4j
class TrustedCert {

    String subject;
    String serialNumber;
    LocalDate validFrom;
    LocalDate validTo;

    public static Optional<TrustedCert> of(X509Certificate x509Certificate) {
        try {
            String subject = x509Certificate.getSubjectX500Principal().getName();
            if (subject.length() > 50) {
                subject = subject.substring(0, 50);
            }
            TrustedCert cert = TrustedCert.builder()
                    .subject(subject)
                    .serialNumber(String.valueOf(x509Certificate.getSerialNumber()))
                    .validFrom(toLocalDate(x509Certificate.getNotBefore()))
                    .validTo(toLocalDate(x509Certificate.getNotAfter()))
                    .build();
            return Optional.of(cert);
        } catch (Exception ex) {
            log.error("Failed to load certificate", ex);
            return Optional.empty();
        }
    }

    private static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }
}
