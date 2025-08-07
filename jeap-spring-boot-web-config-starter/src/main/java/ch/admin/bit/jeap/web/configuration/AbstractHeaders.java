package ch.admin.bit.jeap.web.configuration;

import org.springframework.http.HttpHeaders;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Adds default security and caching headers, optimized for single-page frontend applications
 */
public abstract class AbstractHeaders<RESPONSE> {

    public static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    public static final String REFERRER_POLICY = "Referrer-Policy";
    public static final String FEATURE_POLICY = "Feature-Policy";
    public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
    public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    public static final String X_FRAME_OPTIONS = "X-Frame-Options";
    public static final String X_XSS_PROTECTION = "X-XSS-Protection";

    /**
     * Pattern to extract the source part of a url (scheme, host and port, excluding any paths
     */
    private static final Pattern CONTENT_SOURCE_FROM_HTTP_URL_PATTERN = Pattern.compile(
            "(?<origin>http[s]?://[^/]+).*");

    private final HttpHeaderFilterPostProcessor postProcessor;
    private final String cspHeaderValue;
    private final String featurePolicyHeaderValue;

    /**
     * @param additionalContentSources If given (nullable), is added to the list of allowed frame/connect sources in the Content-Security-Policy header
     * @param contentSecurityPolicy
     * @param featurePolicy if given, then overrides the default value of the feature-policy header
     */
    protected AbstractHeaders(HttpHeaderFilterPostProcessor postProcessor, Collection<String> additionalContentSources, String contentSecurityPolicy, String featurePolicy) {
        this.postProcessor = postProcessor;
        this.cspHeaderValue = createCspHeaderValue(additionalContentSources, contentSecurityPolicy);
        this.featurePolicyHeaderValue = createFeaturePolicyHeaderValue(featurePolicy);
    }

    protected abstract void setHeadersMapToResponse(RESPONSE response, Map<String, String> headers);

    private String createCspHeaderValue(Collection<String> additionalContentSources, String contentSecurityPolicy) {
        if (contentSecurityPolicy != null && !contentSecurityPolicy.isBlank()) {
            return contentSecurityPolicy;
        }

        String contentSourceValuePostfix = "";
        if (additionalContentSources != null && !additionalContentSources.isEmpty()) {

            contentSourceValuePostfix = " " + additionalContentSources.stream()
                    .map(AbstractHeaders::httpUrlToContentSource)
                    .collect(Collectors.joining(" "));
        }

        return String.format("default-src 'none'; " +
                "script-src 'self'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "font-src 'self'; " +
                "img-src 'self'; " +
                "connect-src 'self'%s; " + // Identity provider URL is added as allowed API endpoint
                "frame-src 'self'%s; " + // Identity provider URL is added as allowed iframe URL (i.e. for silent refresh of tokens)
                "frame-ancestors 'self'", contentSourceValuePostfix, contentSourceValuePostfix);
    }

    private String createFeaturePolicyHeaderValue(String featurePolicy) {
        if (featurePolicy != null && !featurePolicy.isBlank()) {
            return featurePolicy;
        }
        return "microphone 'none'; payment 'none'; camera 'none'";
    }

    /**
     * Santitizes Content-Security-Policy values according to <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/Sources#sources">source-value</a>.
     * <p>
     * Mostly meant to allow for URLs to be used as additionalContentSource values (i.e. Keycloak Realm), from which
     * the host part is then used as origin.
     */
    static String httpUrlToContentSource(String contentSource) {
        boolean isLiteralValue = contentSource.startsWith("'"); // 'self' etc.
        boolean isSchemeSource = contentSource.endsWith(":"); // blob: etc.
        if (isLiteralValue || isSchemeSource) {
            return contentSource;
        }

        Matcher matcher = CONTENT_SOURCE_FROM_HTTP_URL_PATTERN.matcher(contentSource);
        if (matcher.matches()) {
            return matcher.group("origin");
        }

        return contentSource;
    }

    public void addHeaders(RESPONSE response, String method, String path) {
        Map<String, String> headers = new LinkedHashMap<>();
        addSecurityHeaders(headers);
        addCachingHeaders(headers, path);

        postProcessor.postProcessHeaders(headers, method, path);

        setHeadersMapToResponse(response, headers);
    }

    private void addSecurityHeaders(Map<String, String> headers) {
        // This header defines which content types may be loaded and from which sources.
        headers.put(CONTENT_SECURITY_POLICY, cspHeaderValue);
        // This header, usually known as HSTS, forces the use of HTTPS.
        headers.put(STRICT_TRANSPORT_SECURITY, "max-age=16070400; includeSubDomains");
        // This headers provides a mechanism to allow or deny the use of browser features
        headers.put(FEATURE_POLICY, featurePolicyHeaderValue);
        // nosniff will prevent browsers from MIME-sniffing a response away from the declared content-type
        headers.put(X_CONTENT_TYPE_OPTIONS, "nosniff");
        // This header governs with referrer information will be included in the Referer header.
        headers.put(REFERRER_POLICY, "strict-origin-when-cross-origin");

        // Headers superseeded by CSP, for backwards compatibility
        // This header prevents the rendering within a frame, iframe or object. This prevents a website of being embedded into another one.
        headers.put(X_FRAME_OPTIONS, "sameorigin");
        // This header prevents XSS (cross site scripting) attacks.
        headers.put(X_XSS_PROTECTION, "1; mode=block");
    }

    private void addCachingHeaders(Map<String, String> headers, String path) {
        if (path.endsWith(".html") || path.endsWith(".json") || path.endsWith("/")) {
            // In an SPA, there should be only 1 html file: index.html, and it should never be cached so that a new
            // version is immediately served. json files are used for translations and should not be cached either to
            // ensure old translations are not served
            headers.put(HttpHeaders.CACHE_CONTROL, "no-cache");
            headers.put(HttpHeaders.EXPIRES, "0");
        } else if (path.endsWith(".js") || path.endsWith(".css")) {
            // Frontend build tools automatically apply a unique hash to each js and css file, meaning a new version
            // will have new files. Therefore, those files will never be changed and can be cached for a long time.
            headers.put(HttpHeaders.CACHE_CONTROL, "public, max-age=15778476, must-revalidate");
            headers.put(HttpHeaders.EXPIRES, "6 months");
        } else {
            // Other static resource are not likely to change, therefore can be cached for a while but must be regularly
            // checked for freshness
            headers.put(HttpHeaders.CACHE_CONTROL, "public, max-age=604800, must-revalidate");
            headers.put(HttpHeaders.EXPIRES, "1 week");
        }

        // Note: Etag headers for static resource are generated by the ShallowEtagHeaderFilter
    }
}
