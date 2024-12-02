package ch.admin.bit.jeap.tls;

import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configure the embedded web-server for TLS using a self-signed certificate created on-the-fly.
 */
public class EnableTlsEnvPostprocessor implements EnvironmentPostProcessor {

    static final String TLS_ENABLED_PROPERTY_NAME = "jeap.web.tls.enabled";
    static final String TLS_HOSTNAME_PROPERTY_NAME = "jeap.web.tls.self-signed-cert.hostname";
    static final String TLS_DAYS_VALID_PROPERTY_NAME = "jeap.web.tls.self-signed-cert.days-valid";
    static final int TLS_DAYS_VALID_DEFAULT = 10 * 365; // 10 years
    static final String TLS_PROPERTY_SOURCE_NAME = "jeap-tls-config";
    static final String SSL_BUNDLE_NAME = "web-server";
    static final String SSL_BUNDLE_KEY_PROPERTY_NAME = "spring.ssl.bundle.pem." + SSL_BUNDLE_NAME + ".keystore.private-key";
    static final String SSL_BUNDLE_CERT_PROPERTY_NAME = "spring.ssl.bundle.pem." + SSL_BUNDLE_NAME + ".keystore.certificate";
    static final String SSL_BUNDLE_PROPERTY_NAME = "server.ssl.bundle";
    static final String HTTP_2_ENABLED_PROPERTY_NAME = "server.http2.enabled";
    static final String SPRING_APPLICATION_NAME_PROPERTY_NAME = "spring.application.name";

    private final Log log;
    private PemKeyCertPair pemKeyCertPair;

    public EnableTlsEnvPostprocessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(getClass());
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (isTlsEnabled(environment)) {
            log.info("jEAP TLS is enabled");
            initializePemKeyCertPair(environment);
            Map<String, Object> tlsConfigProperties = new HashMap<>();
            tlsConfigProperties.put(SSL_BUNDLE_KEY_PROPERTY_NAME, pemKeyCertPair.key());
            tlsConfigProperties.put(SSL_BUNDLE_CERT_PROPERTY_NAME, pemKeyCertPair.cert());
            tlsConfigProperties.put(SSL_BUNDLE_PROPERTY_NAME, SSL_BUNDLE_NAME);
            tlsConfigProperties.put(HTTP_2_ENABLED_PROPERTY_NAME, "true");
            addPropertiesAsPropertySource(environment, tlsConfigProperties, TLS_PROPERTY_SOURCE_NAME);
        } else {
            log.info("jEAP TLS is disabled");
        }
    }

    private boolean isTlsEnabled(Environment environment) {
        return environment.getProperty(TLS_ENABLED_PROPERTY_NAME, "true").equalsIgnoreCase("true");
    }
    private void initializePemKeyCertPair(Environment environment) {
        if (pemKeyCertPair == null) {
            String hostName = getHostname(environment);
            Duration validity = getValidity(environment);
            log.info("Creating a self-signed certificate for hostname '%s' valid '%s' days."
                    .formatted(hostName, validity.toDays()));
            pemKeyCertPair = PemKeyCertPairFactory.createPemKeyCertPair(hostName, validity);
        }
    }

    private String getHostname(Environment environment) {
        final String configuredHostname = environment.getProperty(TLS_HOSTNAME_PROPERTY_NAME);
        if (StringUtils.hasText(configuredHostname)) {
            return configuredHostname;
        } else {
            return getSpringApplicationName(environment);
        }
    }

    private String getSpringApplicationName(Environment environment) {
        return environment.getProperty(SPRING_APPLICATION_NAME_PROPERTY_NAME, "unknown");
    }

    private Duration getValidity(Environment environment) {
        String daysValidStr = environment.getProperty(TLS_DAYS_VALID_PROPERTY_NAME);
        if (StringUtils.hasText(daysValidStr)) {
            try {
                return Duration.ofDays(Integer.parseInt(daysValidStr));
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot parse property %s as an int from given value '%s'.".formatted(
                        TLS_DAYS_VALID_PROPERTY_NAME, daysValidStr), e);
            }
        } else {
            return Duration.ofDays(TLS_DAYS_VALID_DEFAULT);
        }
    }

    private void addPropertiesAsPropertySource(ConfigurableEnvironment environment, Map<String, Object> properties, String propertySourceName) {
        if (StringUtils.hasText(environment.getProperty("server.ssl.bundle"))) {
            log.debug("Server SSL bundle already configured.");
        } else {
            log.info("Adding config properties (%s) to environment in config source %s.".formatted(
                    String.join(", ", properties.keySet()), propertySourceName));
            environment.getPropertySources().addLast(new MapPropertySource(propertySourceName, properties));
        }
    }

}
