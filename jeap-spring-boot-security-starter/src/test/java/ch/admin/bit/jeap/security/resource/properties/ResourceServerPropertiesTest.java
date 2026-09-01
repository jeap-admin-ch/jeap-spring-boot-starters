package ch.admin.bit.jeap.security.resource.properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext.*;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("authprops")
@EnableConfigurationProperties({ResourceServerProperties.class})
@SpringBootTest(classes = ResourceServerProperties.class)
public class ResourceServerPropertiesTest {

    @Autowired
    private ResourceServerProperties resourceServerProperties;

    @Test
    void testResourceServerProperties() {
        AuthorizationServerConfigProperties authServer = resourceServerProperties.getAuthorizationServer();
        B2BGatewayConfigProperties b2bGateway = resourceServerProperties.getB2BGateway();
        List<AuthorizationServerConfigProperties> authServers = resourceServerProperties.getAuthServers();

        assertThat(resourceServerProperties.getResourceId()).isEqualTo("test-resource");
        assertThat(resourceServerProperties.getApplicationName()).isEqualTo("test-app");
        assertThat(resourceServerProperties.getStrictAudienceValidation()).isEqualTo(StrictAudienceValidationMode.ON);

        assertThat(authServer.getIssuer()).isEqualTo("http://keycloak/auth/realm");
        assertThat(authServer.getJwkSetUri()).isEqualTo("http://keycloak/auth/realm/protocol/openid-connect/certs");
        assertThat(authServer.getClaimSetConverterName()).isNull();
        assertThat(authServer.getAuthenticationContexts()).containsOnly(SYS, USER);
        assertThat(authServer.getJwksConnectTimeoutInMillis()).isEqualTo(3000);
        assertThat(authServer.getJwksReadTimeoutInMillis()).isEqualTo(4000);

        assertThat(b2bGateway.getIssuer()).isEqualTo("http://b2b/auth");
        assertThat(b2bGateway.getJwkSetUri()).isEqualTo("http://b2b/.well-known/jwks.json");
        assertThat(b2bGateway.getClaimSetConverterName()).isEqualTo("test-b2b-converter");
        assertThat(b2bGateway.getAuthenticationContexts()).containsOnly(B2B);
        assertThat(b2bGateway.getJwksConnectTimeoutInMillis()).isEqualTo(1000);
        assertThat(b2bGateway.getJwksReadTimeoutInMillis()).isEqualTo(2000);

        assertThat(authServers).hasSize(3);

        AuthorizationServerConfigProperties keykloak2 = authServers.getFirst();
        assertThat(keykloak2.getIssuer()).isEqualTo("http://keycloak2/auth/realm");
        assertThat(keykloak2.getJwkSetUri()).isEqualTo("http://keycloak2/auth/realm/protocol/openid-connect/certs");
        assertThat(keykloak2.getClaimSetConverterName()).isEqualTo("test-keycloak2-converter");
        assertThat(keykloak2.getAuthenticationContexts()).containsOnly(SYS, USER);
        assertThat(keykloak2.getJwksConnectTimeoutInMillis()).isEqualTo(15000); // default
        assertThat(keykloak2.getJwksReadTimeoutInMillis()).isEqualTo(15000); // default

        AuthorizationServerConfigProperties keykloak3 = authServers.get(1);
        assertThat(keykloak3.getIssuer()).isEqualTo("http://keycloak3/auth/realm");
        assertThat(keykloak3.getJwkSetUri()).isEqualTo("http://keycloak3/auth/realm/jwks.json");
        assertThat(keykloak3.getClaimSetConverterName()).isNull();
        assertThat(keykloak3.getAuthenticationContexts()).containsOnly(USER);

        AuthorizationServerConfigProperties b2bGateway2 = authServers.get(2);
        assertThat(b2bGateway2.getIssuer()).isEqualTo("http://b2b2/auth");
        assertThat(b2bGateway2.getJwkSetUri()).isEqualTo("http://b2b/.well-known/jwks.json");
        assertThat(b2bGateway2.getClaimSetConverterName()).isNull();
        assertThat(b2bGateway2.getAuthenticationContexts()).containsOnly(B2B);

        var allAuthServerConfigurations = resourceServerProperties.getAllAuthServerConfigurations();
        assertThat(allAuthServerConfigurations).hasSize(5);
        assertThat(allAuthServerConfigurations.get(0)).isEqualTo(authServer);
        // the B2B gateway configuration is converted to an auth server configuration, all values must be carried over
        AuthorizationServerConfigProperties b2bGatewayAsAuthServer = allAuthServerConfigurations.get(1);
        assertThat(b2bGatewayAsAuthServer.getIssuer()).isEqualTo("http://b2b/auth");
        assertThat(b2bGatewayAsAuthServer.getJwkSetUri()).isEqualTo("http://b2b/.well-known/jwks.json");
        assertThat(b2bGatewayAsAuthServer.getClaimSetConverterName()).isEqualTo("test-b2b-converter");
        assertThat(b2bGatewayAsAuthServer.getAuthenticationContexts()).containsOnly(B2B);
        assertThat(b2bGatewayAsAuthServer.getJwksConnectTimeoutInMillis()).isEqualTo(1000);
        assertThat(b2bGatewayAsAuthServer.getJwksReadTimeoutInMillis()).isEqualTo(2000);
        assertThat(allAuthServerConfigurations.get(2)).isEqualTo(keykloak2);
        assertThat(allAuthServerConfigurations.get(3)).isEqualTo(keykloak3);
        assertThat(allAuthServerConfigurations.get(4)).isEqualTo(b2bGateway2);
    }

    @Test
    void testResourceIdDefaultsToApplicationNameWhenNotSetExplicitly() {
        ResourceServerProperties properties = new ResourceServerProperties();
        properties.setApplicationName("test-app");

        properties.initialize();

        assertThat(properties.getResourceId()).isEqualTo("test-app");
    }

}
