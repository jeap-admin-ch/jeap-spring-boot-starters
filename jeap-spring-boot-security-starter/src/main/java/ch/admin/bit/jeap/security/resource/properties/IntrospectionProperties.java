package ch.admin.bit.jeap.security.resource.properties;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Slf4j
public class IntrospectionProperties {

    private static final String INTROSPECTION_URL_SUFFIX = "protocol/openid-connect/token/introspect";

    /**
     * URI of the token introspection endpoint. Optional.
     * If not set, the uri is derived from the configured issuer uri.
     */
    String uri;

    /**
     * ID of the confidential client to access the token introspection endpoint. Required.
     */
    @NotEmpty
    String clientId;

    /**
     * Secret of the confidential client to access the token introspection endpoint. Required.
     */
    @NotEmpty
    String clientSecret;

    /**
     * Connect timeout for a token introspection http request in milliseconds.
     */
    int connectTimeoutInMillis = 15000;

    /**
     * Read timeout for a token introspection http request in milliseconds.
     */
    int readTimeoutInMillis = 15000;

    public void validate(String issuerUri) {
        log.info("Validating introspection properties for uri {} and clientId {}", uri, clientId);
        if (this.uri == null || this.uri.isEmpty()) {
            this.uri = ensureTrailingSlash(issuerUri) + INTROSPECTION_URL_SUFFIX;
            log.info("No token introspection URI specified for issuer '{}'. Using issuer uri to derive the introspection uri '{}'", issuerUri, this.uri);
        }
    }

    private String ensureTrailingSlash(String uri) {
        if (!uri.endsWith("/")) {
            return uri + "/";
        }
        return uri;
    }

}
