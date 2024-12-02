package ch.admin.bit.jeap.tls;

import lombok.SneakyThrows;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import java.io.StringReader;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnableTlsEnvPostprocessorTest {

    private EnableTlsEnvPostprocessor enableTlsEnvPostprocessor;
    private MockEnvironment mockEnvironment = new MockEnvironment();

    @BeforeEach
    void setUp() {
        enableTlsEnvPostprocessor = new EnableTlsEnvPostprocessor(new DeferredLogs());
        mockEnvironment = new MockEnvironment();
        mockEnvironment.setProperty(EnableTlsEnvPostprocessor.SPRING_APPLICATION_NAME_PROPERTY_NAME, "test-app");
    }

    @Test
    void testPostProcessEnvironment_whenDisabled_thenPropertiesNotSet() {
        mockEnvironment.setProperty(EnableTlsEnvPostprocessor.TLS_ENABLED_PROPERTY_NAME, "false");

        enableTlsEnvPostprocessor.postProcessEnvironment(mockEnvironment, null);

        assertThat(mockEnvironment.getProperty(EnableTlsEnvPostprocessor.SSL_BUNDLE_PROPERTY_NAME)).isNull();
        assertThat(mockEnvironment.getProperty(EnableTlsEnvPostprocessor.SSL_BUNDLE_KEY_PROPERTY_NAME)).isNull();
        assertThat(mockEnvironment.getProperty(EnableTlsEnvPostprocessor.SSL_BUNDLE_CERT_PROPERTY_NAME)).isNull();
    }

    @Test
    void testPostProcessEnvironment_whenEnabled_thenPropertiesSet() {
        final int daysValid = 365;
        mockEnvironment.setProperty(EnableTlsEnvPostprocessor.TLS_ENABLED_PROPERTY_NAME, "true");
        mockEnvironment.setProperty(EnableTlsEnvPostprocessor.TLS_DAYS_VALID_PROPERTY_NAME, Integer.toString(daysValid));

        enableTlsEnvPostprocessor.postProcessEnvironment(mockEnvironment, null);

        assertSslPropertiesSet(mockEnvironment, daysValid);
    }

    @Test
    void testPostProcessEnvironment_whenDefault_thenPropertiesSet() {
        enableTlsEnvPostprocessor.postProcessEnvironment(mockEnvironment, null);

        assertSslPropertiesSet(mockEnvironment, EnableTlsEnvPostprocessor.TLS_DAYS_VALID_DEFAULT);
    }

    @Test
    void testPostProcessEnvironment_whenPropertiesAlreadySet_thenDontSet() {
        mockEnvironment.setProperty(EnableTlsEnvPostprocessor.SSL_BUNDLE_PROPERTY_NAME, "some-bundle");

        enableTlsEnvPostprocessor.postProcessEnvironment(mockEnvironment, null);

        assertThat(mockEnvironment.getProperty(EnableTlsEnvPostprocessor.SSL_BUNDLE_PROPERTY_NAME)).isEqualTo("some-bundle");
        assertThat(mockEnvironment.getProperty(EnableTlsEnvPostprocessor.SSL_BUNDLE_KEY_PROPERTY_NAME)).isNull();
        assertThat(mockEnvironment.getProperty(EnableTlsEnvPostprocessor.SSL_BUNDLE_CERT_PROPERTY_NAME)).isNull();
        assertThat(mockEnvironment.getProperty(EnableTlsEnvPostprocessor.HTTP_2_ENABLED_PROPERTY_NAME)).isNull();
    }

    @SneakyThrows
    private static void assertSslPropertiesSet(Environment environment, int daysValid) {
        assertThat(environment.getProperty(EnableTlsEnvPostprocessor.SSL_BUNDLE_PROPERTY_NAME)).isEqualTo(EnableTlsEnvPostprocessor.SSL_BUNDLE_NAME);
        assertThat(environment.getProperty(EnableTlsEnvPostprocessor.SSL_BUNDLE_KEY_PROPERTY_NAME)).isNotEmpty();
        String pemCert = environment.getProperty(EnableTlsEnvPostprocessor.SSL_BUNDLE_CERT_PROPERTY_NAME);
        assertThat(pemCert).isNotEmpty();
        assertThat(environment.getProperty(EnableTlsEnvPostprocessor.HTTP_2_ENABLED_PROPERTY_NAME)).isEqualTo("true");

        PemObject po = new PEMParser(new StringReader(pemCert)).readPemObject();
        X509CertificateHolder certificateHolder = new X509CertificateHolder(po.getContent());
        JcaX509CertificateConverter x509Converter = new JcaX509CertificateConverter();
        X509Certificate certificate = x509Converter.getCertificate(certificateHolder);

        // assert certificate validity
        OffsetDateTime validUpTo = OffsetDateTime.now().plusDays(daysValid);
        certificate.checkValidity(Date.from(validUpTo.minusMinutes(1).toInstant()));
        assertThatThrownBy(() -> certificate.checkValidity(Date.from(validUpTo.plusMinutes(2).toInstant()))).
                isInstanceOf(CertificateExpiredException.class);

        // assert certificate subject name set
        final String springApplicationName = environment.getProperty(EnableTlsEnvPostprocessor.SPRING_APPLICATION_NAME_PROPERTY_NAME);
        assertThat(certificate.getSubjectX500Principal().getName()).isEqualTo("CN=" + springApplicationName);

        // assert certificate subject alternative name set
        List<?> subjectAltName = certificate.getSubjectAlternativeNames().iterator().next();
        assertThat(subjectAltName.get(0)).isEqualTo(2);
        assertThat(subjectAltName.get(1)).isEqualTo(springApplicationName);
    }

}
