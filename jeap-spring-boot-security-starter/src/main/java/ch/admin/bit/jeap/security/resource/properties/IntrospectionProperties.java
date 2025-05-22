package ch.admin.bit.jeap.security.resource.properties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Data
@Slf4j
public class IntrospectionProperties {

    private static final String INTROSPECTION_URL_SUFFIX = "protocol/openid-connect/token/introspect";

    /**
     * URI of the token introspection endpoint. Optional.
     * If not set, the uri is derived from the configured issuer uri.
     */
    String uri;

    /**
     * ID of the confidential client to access the token introspection endpoint.
     * Required if introspection is not disabled.
     */
    String clientId;

    /**
     * Secret of the confidential client to access the token introspection endpoint.
     * Required if introspection is not disabled.
     */
    String clientSecret;

    /**
     * Connect timeout for a token introspection http request in milliseconds.
     */
    int connectTimeoutInMillis = 15000;

    /**
     * Read timeout for a token introspection http request in milliseconds.
     */
    int readTimeoutInMillis = 15000;

    /**
     * You can disable introspection on this authorization server by setting this property to {@link IntrospectionMode#NONE}
     */
    IntrospectionMode mode;

    public void validate(String issuerUri) {
        log.info("Validating introspection properties for issuer {}.", uri);
        if (mode == IntrospectionMode.NONE) {
            // Introspection is disabled -> no need to validate the properties
            return;
        }
        if (mode != null) {
            throw new IllegalStateException("""
                    Configuring an introspection mode other than 'NONE' is not supported on the authorization server level. \
                    Please remove the introspection mode '%s' from the authorization server configuration for the issuer '%s' \
                    or set the mode to 'NONE'.""".formatted(mode, issuerUri));
        }
        if (!StringUtils.hasText(this.uri)) {
            this.uri = ensureTrailingSlash(issuerUri) + INTROSPECTION_URL_SUFFIX;
            log.info("No token introspection URI specified for issuer '{}'. Using issuer uri to derive the introspection uri '{}'", issuerUri, this.uri);
        }
        if (!StringUtils.hasText(this.clientId)) {
            throw new IllegalArgumentException("client-id must be provided");
        }
        if (!StringUtils.hasText(this.clientSecret)) {
            throw new IllegalArgumentException("client-secret must be provided");
        }
    }

    private String ensureTrailingSlash(String uri) {
        if (!uri.endsWith("/")) {
            return uri + "/";
        }
        return uri;
    }

}
