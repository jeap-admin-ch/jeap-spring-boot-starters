package ch.admin.bit.jeap.security.resource.properties;

import jakarta.validation.Valid;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
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
     * ID of the confidential client to access the token introspection endpoint. Optional.
     * If not set, the resource id of this resource server is used (jeap.security.oauth2.resourceserver.resource-id,
     * defaulting to spring.application.name), as the Keycloak setup requires the introspection client id to be
     * identical to the resource id.
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

    /**
     * Local caching of the token introspection responses of this authorization server. Disabled by default.
     */
    @Valid
    @NestedConfigurationProperty
    IntrospectionCacheProperties cache = new IntrospectionCacheProperties();

    /**
     * Whether these properties disable introspection for the authorization server they belong to, i.e. whether an
     * introspection mode that does not activate introspection (NONE) has been configured. No mode configured means "not
     * disabled", as the introspection mode is then the one configured on the resource server level.
     */
    public boolean isIntrospectionDeactivated() {
        return mode != null && !mode.doesActivateIntrospection();
    }

    /**
     * Whether the introspection responses of the authorization server these properties belong to are to be cached.
     */
    public boolean isCacheEnabled() {
        return cache != null && cache.isEnabled();
    }

    /**
     * Complete these introspection properties with derived defaults for the values that have not been configured:
     * the introspection uri is derived from the issuer uri and the client id defaults to the resource id.
     * Does nothing if introspection is disabled for this authorization server.
     *
     * @param issuerUri   The issuer uri of the authorization server these introspection properties belong to.
     * @param resourceId  The resource server's resource id.
     */
    public void applyDefaults(String issuerUri, String resourceId) {
        if (isIntrospectionDeactivated()) {
            return;
        }
        if (!StringUtils.hasText(this.uri) && StringUtils.hasText(issuerUri)) {
            this.uri = ensureTrailingSlash(issuerUri) + INTROSPECTION_URL_SUFFIX;
            log.info("No token introspection URI specified for issuer '{}'. Using issuer uri to derive the introspection uri '{}'", issuerUri, this.uri);
        }
        if (!StringUtils.hasText(this.clientId) && StringUtils.hasText(resourceId)) {
            this.clientId = resourceId;
            log.info("No token introspection client id specified for issuer '{}'. Using the resource id '{}' as introspection client id.", issuerUri, this.clientId);
        }
    }

    /**
     * Validate these introspection properties. Expects the defaults to have been applied already (see
     * {@link #applyDefaults(String, String)}).
     *
     * @param issuerUri The issuer uri of the authorization server these introspection properties belong to.
     */
    public void validate(String issuerUri) {
        if (isIntrospectionDeactivated()) {
            // no need to validate the properties
            return;
        }
        log.info("Validating introspection properties for issuer {}.", issuerUri);
        if (mode != null) {
            throw new IllegalStateException("""
                    Configuring an introspection mode other than 'NONE' is not supported on the authorization server level. \
                    Please remove the introspection mode '%s' from the authorization server configuration for the issuer '%s' \
                    or set the mode to 'NONE'.""".formatted(mode, issuerUri));
        }
        if (!StringUtils.hasText(this.clientId)) {
            throw new IllegalArgumentException("client-id must be provided");
        }
        if (!StringUtils.hasText(this.clientSecret)) {
            throw new IllegalArgumentException("client-secret must be provided");
        }
        if (cache != null) {
            cache.validate(issuerUri);
        }
    }

    private String ensureTrailingSlash(String uri) {
        if (!uri.endsWith("/")) {
            return uri + "/";
        }
        return uri;
    }

}
