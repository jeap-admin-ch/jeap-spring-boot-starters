package ch.admin.bit.jeap.security.resource.properties;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import lombok.Data;
import org.springframework.util.StringUtils;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

import static ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext.SYS;
import static ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext.USER;

/**
 * Configuration properties to configure the authorization server that the OAuth2 resource server will accept tokens from.
 */
@Data
public class AuthorizationServerConfigProperties implements AuthorizationServerConfiguration {

    private static final String JWK_SET_URI_SUBPATH = "/protocol/openid-connect/certs";

    /**
     * Issuer of the token
     */
    @NotBlank
    private String issuer;

    /**
     * URL to get token signer certificate from
     */
    private String jwkSetUri;

    /**
     * The jEAP authentication contexts allowed for tokens from this authorization sever. Defaults to USER and SYS.
     */
    @NotEmpty
    private Set<JeapAuthenticationContext> authenticationContexts = Set.of(USER, SYS);

    /**
     * Name of the claim set converter bean to use
     */
    private String claimSetConverterName;

    /**
     * Timeout in milliseconds for connecting to the JWK set URI
     */
    private int jwksConnectTimeoutInMillis = 15_000;

    /**
     * Timeout in milliseconds for reading the JWK set URI
     */
    private int jwksReadTimeoutInMillis = 15_000;

    public String getJwkSetUri() {
        return StringUtils.hasText(jwkSetUri) ? jwkSetUri : issuer + JWK_SET_URI_SUBPATH;
    }

}

