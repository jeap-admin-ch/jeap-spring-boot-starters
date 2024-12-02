package ch.admin.bit.jeap.monitor.metrics.truststore;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Slf4j
@Getter
class TrustStoreService {

    static final String JAVAX_NET_SSL_TRUST_STORE = "javax.net.ssl.trustStore";
    static final String JAVAX_NET_SSL_TRUST_STORE_PASSWORD = "javax.net.ssl.trustStorePassword";

    private List<TrustedCert> trustedCerts = List.of();

    public TrustStoreService() {
        KeyStore ks = loadKeyStore();
        if (ks == null) {
            return;
        }
        List<String> aliases = listCertificateAliases(ks);

        trustedCerts = aliases.stream()
                .flatMap(alias -> loadCert(ks, alias).stream())
                .toList();
    }

    private static List<String> listCertificateAliases(KeyStore ks) {
        try {
            return Collections.list(ks.aliases());
        } catch (KeyStoreException ex) {
            log.error("Failed to list keystore", ex);
            return List.of();
        }
    }

    private Optional<TrustedCert> loadCert(KeyStore ks, String alias) {
        try {
            Certificate certificate = ks.getCertificate(alias);
            if (certificate instanceof X509Certificate) {
                return TrustedCert.of((X509Certificate) certificate);
            }
        } catch (Exception ex) {
            log.error("Failed to load certificate " + alias, ex);
        }
        return Optional.empty();
    }

    private KeyStore loadKeyStore() {
        String path = System.getProperty(JAVAX_NET_SSL_TRUST_STORE);
        String passPhrase = System.getProperty(JAVAX_NET_SSL_TRUST_STORE_PASSWORD);

        if (path == null || passPhrase == null) {
            log.info("No trustStore system properties set - will not provide certificate metrics");
            return null;
        }

        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(path), passPhrase.toCharArray());
            return ks;
        } catch (Exception ex) {
            log.error("Failed to load trust store from " + path, ex);
            return null;
        }
    }
}