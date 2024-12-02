package ch.admin.bit.jeap.web.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractHeadersTest {

    private AbstractHeaders<Map<String, String>> headers;

    @Test
    void httpUrlToContentSource() {
        assertEquals("'self'", AbstractHeaders.httpUrlToContentSource("'self'"));
        assertEquals("blob:", AbstractHeaders.httpUrlToContentSource("blob:"));
        assertEquals("https://foo-bar.com", AbstractHeaders.httpUrlToContentSource("https://foo-bar.com/path"));
        assertEquals("https://foo-bar.com", AbstractHeaders.httpUrlToContentSource("https://foo-bar.com/"));
        assertEquals("https://foo-bar.com", AbstractHeaders.httpUrlToContentSource("https://foo-bar.com"));
        assertEquals("http://foo-bar.com", AbstractHeaders.httpUrlToContentSource("http://foo-bar.com"));
        assertEquals("https://foo-bar.com:443", AbstractHeaders.httpUrlToContentSource("https://foo-bar.com:443"));
        assertEquals("https://foo-bar.com:443", AbstractHeaders.httpUrlToContentSource("https://foo-bar.com:443/path"));
        assertEquals("https://ref-identity.test.ch",
                AbstractHeaders.httpUrlToContentSource("https://ref-identity.test.ch/auth/realms/bit-jme"));
    }

    @Test
    void addCachingHeaders_rootUrl() {
        Map<String, String> responseStub = new HashMap<>();
        headers.addHeaders(responseStub, "GET", "/");

        assertEquals("no-cache", responseStub.get("Cache-Control"));
        assertEquals("0", responseStub.get("Expires"));
    }

    @Test
    void addCachingHeaders_html() {
        Map<String, String> responseStub = new HashMap<>();
        headers.addHeaders(responseStub, "GET", "/index.html");

        assertEquals("no-cache", responseStub.get("Cache-Control"));
        assertEquals("0", responseStub.get("Expires"));
    }

    @Test
    void addCachingHeaders_js() {
        Map<String, String> responseStub = new HashMap<>();
        headers.addHeaders(responseStub, "GET", "/my-234.js");

        assertEquals("public, max-age=15778476, must-revalidate", responseStub.get("Cache-Control"));
        assertEquals("6 months", responseStub.get("Expires"));
    }

    @Test
    void addCachingHeaders_static_resource() {
        Map<String, String> responseStub = new HashMap<>();
        headers.addHeaders(responseStub, "GET", "/my-234.ong");

        assertEquals("public, max-age=604800, must-revalidate", responseStub.get("Cache-Control"));
        assertEquals("1 week", responseStub.get("Expires"));
    }

    @Test
    void addSecurityHeaders() {
        Map<String, String> responseStub = new HashMap<>();
        headers.addHeaders(responseStub, "GET", "/index.html");

        assertEquals("default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; font-src 'self';" +
                        " img-src 'self'; connect-src 'self' http://test; frame-src 'self' http://test; frame-ancestors 'self'",
                responseStub.get("Content-Security-Policy"));
        assertEquals("max-age=16070400; includeSubDomains", responseStub.get("Strict-Transport-Security"));
        assertEquals("microphone 'none'; payment 'none'; camera 'none'", responseStub.get("Feature-Policy"));
        assertEquals("nosniff", responseStub.get("X-Content-Type-Options"));
        assertEquals("sameorigin", responseStub.get("X-Frame-Options"));
        assertEquals("1; mode=block", responseStub.get("X-XSS-Protection"));
    }

    @Test
    void post_processor() {
        AbstractHeaders<Map<String, String>> headers = createHeadersInstanceWithPostProcessor();

        Map<String, String> responseStub = new HashMap<>();
        headers.addHeaders(responseStub, "GET", "/");

        assertEquals("test", responseStub.get("Custom-Header"));
    }

    @BeforeEach
    void setUp() {
        Collection<String> additionalSources = Set.of("http://test/cut/me/off");
        headers = new AbstractHeaders<>(HttpHeaderFilterPostProcessor.NO_OP, additionalSources, null) {
            @Override
            protected void setHeadersMapToResponse(Map<String, String> target, Map<String, String> headers) {
                target.putAll(headers);
            }
        };
    }

    private AbstractHeaders<Map<String, String>> createHeadersInstanceWithPostProcessor() {
        HttpHeaderFilterPostProcessor postProcessor = new HttpHeaderFilterPostProcessor() {
            @Override
            public void postProcessHeaders(Map<String, String> headers, String method, String path) {
                headers.put("Custom-Header", "test");
            }
        };
        AbstractHeaders<Map<String, String>> headers = new AbstractHeaders<>(postProcessor, Set.of(), null) {
            @Override
            protected void setHeadersMapToResponse(Map<String, String> target, Map<String, String> headers) {
                target.putAll(headers);
            }
        };
        return headers;
    }
}
