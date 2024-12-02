package ch.admin.bit.jeap.security.resource.properties;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

import static ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext.B2B;

/**
 * Configuration properties to configure the B2B gateway that the OAuth2 resource server will accept tokens from.
 */
@Data
public class B2BGatewayConfigProperties implements AuthorizationServerConfiguration {
    /**
     * Issuer of the token
     */
    @NotBlank
    private String issuer;

    /**
     * URL to get token signer certificate from
     */
    @NotBlank
    private String jwkSetUri;

    /**
     * The jEAP authentication contexts allowed for tokens from this authorization sever. Defaults to B2B.
     */
    @NotEmpty
    private Set<JeapAuthenticationContext> authenticationContexts = Set.of(B2B);

    /**
     * Name of the claim set converter bean to use
     */
    private String claimSetConverterName;

    AuthorizationServerConfigProperties asAuthorizationServerConfigProperties() {
        AuthorizationServerConfigProperties configProperties = new AuthorizationServerConfigProperties();
        configProperties.setIssuer(issuer);
        configProperties.setJwkSetUri(jwkSetUri);
        configProperties.setAuthenticationContexts(Set.copyOf(authenticationContexts));
        configProperties.setClaimSetConverterName(claimSetConverterName);
        return configProperties;
    }

}

