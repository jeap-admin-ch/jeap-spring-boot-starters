package ch.admin.bit.jeap.swagger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpsServerBaseUrlConfigurationTest {

    public static final HttpsServerBaseUrlConfiguration.HttpsEnforcerServerBaseUrlCustomizer HTTPS_ENFORCER_SERVER_BASE_URL_CUSTOMIZER = new HttpsServerBaseUrlConfiguration.HttpsEnforcerServerBaseUrlCustomizer();

    @Test
    void testEnforceHttps() {
        String customizedUrl = HTTPS_ENFORCER_SERVER_BASE_URL_CUSTOMIZER.customize("http://myService/", null);

        assertEquals("https://myService/", customizedUrl);
    }

    @Test
    void testIgnoreAlreadySet() {
        String customizedUrl = HTTPS_ENFORCER_SERVER_BASE_URL_CUSTOMIZER.customize("https://myService/", null);

        assertEquals("https://myService/", customizedUrl);
    }

    @Test
    void testIgnoreLocalhost() {
        String customizedUrl = HTTPS_ENFORCER_SERVER_BASE_URL_CUSTOMIZER.customize("http://localhost/", null);

        assertEquals("http://localhost/", customizedUrl);
    }

    @Test
    void testIgnoreLocalhostInHostname() {
        String customizedUrl = HTTPS_ENFORCER_SERVER_BASE_URL_CUSTOMIZER.customize("http://localhost.com/", null);

        assertEquals("https://localhost.com/", customizedUrl);
    }

    @Test
    void testIgnoreLocalhostNotInHostname() {
        String customizedUrl = HTTPS_ENFORCER_SERVER_BASE_URL_CUSTOMIZER.customize("http://my-service/localhost/", null);

        assertEquals("https://my-service/localhost/", customizedUrl);
    }
}