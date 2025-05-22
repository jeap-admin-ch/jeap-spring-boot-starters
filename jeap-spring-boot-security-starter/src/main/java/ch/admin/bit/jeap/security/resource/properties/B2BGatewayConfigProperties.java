package ch.admin.bit.jeap.security.resource.properties;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import jakarta.validation.Valid;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

import static ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext.B2B;

/**
 * Configuration properties to configure the B2B gateway that the OAuth2 resource server will accept tokens from.
 */
@Data
@Validated
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

    /**
     * Timeout in milliseconds for connecting to the JWK set URI
     */
    private int jwksConnectTimeoutInMillis = 15_000;

    /**
     * Timeout in milliseconds for reading the JWK set URI
     */
    private int jwksReadTimeoutInMillis = 15_000;

    /**
     * The introspection configuration related to this server
     */
    @Valid
    @NestedConfigurationProperty
    private IntrospectionProperties introspection;

    AuthorizationServerConfigProperties asAuthorizationServerConfigProperties() {
        AuthorizationServerConfigProperties configProperties = new AuthorizationServerConfigProperties();
        configProperties.setIssuer(issuer);
        configProperties.setJwkSetUri(jwkSetUri);
        configProperties.setAuthenticationContexts(Set.copyOf(authenticationContexts));
        configProperties.setClaimSetConverterName(claimSetConverterName);
        configProperties.setIntrospection(introspection);
        return configProperties;
    }

}

