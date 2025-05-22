package ch.admin.bit.jeap.security.resource.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("introspection")
@EnableConfigurationProperties({ResourceServerProperties.class})
@SpringBootTest(classes = ResourceServerProperties.class)
@ExtendWith(OutputCaptureExtension.class)
class ResourceServerPropertiesIntrospectionTest {

    @Autowired
    private ResourceServerProperties resourceServerProperties;

    @Test
    void loadIntrospectionProperties_allOk() {
        AuthorizationServerConfigProperties authServer = resourceServerProperties.getAuthorizationServer();
        B2BGatewayConfigProperties b2bGateway = resourceServerProperties.getB2BGateway();
        List<AuthorizationServerConfigProperties> authServers = resourceServerProperties.getAuthServers();

        assertThat(resourceServerProperties.getIntrospection().getMode()).isEqualTo(IntrospectionMode.ALWAYS);

        assertThat(authServer.getIntrospection().getClientId()).isEqualTo("myId");
        assertThat(authServer.getIntrospection().getClientSecret()).isEqualTo("mySecret");
        assertThat(authServer.getIntrospection().getUri()).isEqualTo("https://keycloak/auth/realm/protocol/openid-connect/token/introspect");
        assertThat(authServer.getIntrospection().getMode()).isNull();

        assertThat(b2bGateway.getIntrospection().getClientId()).isEqualTo("myB2bId");
        assertThat(b2bGateway.getIntrospection().getClientSecret()).isEqualTo("myB2bSecret");
        assertThat(b2bGateway.getIntrospection().getUri()).isEqualTo("https://b2b/auth/protocol/openid-connect/token/introspect");
        assertThat(b2bGateway.getIntrospection().getMode()).isNull();

        assertThat(authServers).hasSize(2);
        AuthorizationServerConfigProperties authServersFirst = authServers.getFirst();
        assertThat(authServersFirst.getIntrospection().getClientId()).isEqualTo("myFirstId");
        assertThat(authServersFirst.getIntrospection().getClientSecret()).isEqualTo("myFirstSecret");
        assertThat(authServersFirst.getIntrospection().getUri()).isEqualTo("https://custom-uri/introspection-uri/protocol/openid-connect/token/introspect");
        assertThat(authServersFirst.getIntrospection().getMode()).isNull();

        AuthorizationServerConfigProperties authServersSecond = authServers.get(1);
        assertThat(authServersSecond.getIntrospection().getMode()).isEqualTo(IntrospectionMode.NONE);
    }

    @ParameterizedTest
    @EnumSource(value = IntrospectionMode.class, names = {"NONE"}, mode = EnumSource.Mode.EXCLUDE)
    void loadIntrospectionProperties_introspectionActivated_introspectionNotDefined_throwsException(IntrospectionMode introspectionMode) {
        ResourceServerProperties props = new ResourceServerProperties();
        setIntrospectionMode(props, introspectionMode);
        AuthorizationServerConfigProperties authorizationServerConfigProperties = new AuthorizationServerConfigProperties();
        authorizationServerConfigProperties.setIssuer("issuer");
        props.setAuthorizationServer(authorizationServerConfigProperties);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, props::validate);
        assertThat(exception.getMessage()).isEqualTo("issuer: introspection configuration must be defined when introspection mode is activated.");
    }

    @Test
    void loadIntrospectionProperties_introspectionModeNull_introspectionDefined_throwsException() {
        ResourceServerProperties props = new ResourceServerProperties();
        AuthorizationServerConfigProperties authorizationServerConfigProperties = new AuthorizationServerConfigProperties();
        authorizationServerConfigProperties.setIssuer("issuer");
        IntrospectionProperties introspectionProperties = new IntrospectionProperties();
        introspectionProperties.setClientId("myId");
        introspectionProperties.setClientSecret("mySecret");
        authorizationServerConfigProperties.setIntrospection(introspectionProperties);
        props.setAuthorizationServer(authorizationServerConfigProperties);
        B2BGatewayConfigProperties b2BGatewayConfigProperties = new B2BGatewayConfigProperties();
        b2BGatewayConfigProperties.setIssuer("issuer-ok");
        IntrospectionProperties b2bIntrospection = new IntrospectionProperties();
        b2BGatewayConfigProperties.setIntrospection(b2bIntrospection);
        props.setB2BGateway(b2BGatewayConfigProperties);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, props::validate);
        assertThat(exception.getMessage()).isEqualTo("issuer: introspection has not been activated but introspection configurations have been provided. Did you forget to activate introspection by setting an introspection mode?");
    }

    @Test
    void loadIntrospectionProperties_introspectionModeNone_introspectionDefined_generateWarning(CapturedOutput capturedOutput) {
        ResourceServerProperties props = new ResourceServerProperties();
        setIntrospectionMode(props, IntrospectionMode.NONE);
        AuthorizationServerConfigProperties authorizationServerConfigProperties = new AuthorizationServerConfigProperties();
        authorizationServerConfigProperties.setIssuer("issuer");
        IntrospectionProperties introspectionProperties = new IntrospectionProperties();
        introspectionProperties.setClientId("myId");
        introspectionProperties.setClientSecret("mySecret");
        authorizationServerConfigProperties.setIntrospection(introspectionProperties);
        props.setAuthorizationServer(authorizationServerConfigProperties);
        props.validate();
        assertThat(capturedOutput.getOut()).contains("issuer: introspection disabled with introspection mode \"NONE\", but introspection configurations provided");
    }

    @Test
    void loadIntrospectionProperties_introspectionUriConfigured() {
        ResourceServerProperties props = new ResourceServerProperties();
        setIntrospectionMode(props, IntrospectionMode.ALWAYS);
        props.setAuthorizationServer(getAuthorizationServerConfigProperties("my-uri"));
        props.validate();
        assertThat(props.getAuthorizationServer().getIntrospection().getUri()).isEqualTo("my-uri");
    }

    @Test
    void loadIntrospectionProperties_introspectionUriNotConfigured_derivedFromIssuerUri() {
        ResourceServerProperties props = new ResourceServerProperties();
        setIntrospectionMode(props, IntrospectionMode.ALWAYS);
        props.setAuthorizationServer(getAuthorizationServerConfigProperties(null));
        props.validate();
        assertThat(props.getAuthorizationServer().getIntrospection().getUri()).isEqualTo("issuer/protocol/openid-connect/token/introspect");
    }

    @Test
    void loadIntrospectionProperties_IntrospectionPropertiesWithJustModeNone_ValidateSuccessfully() {
        ResourceServerProperties props = new ResourceServerProperties();
        setIntrospectionMode(props, IntrospectionMode.ALWAYS);
        props.setAuthorizationServer(getAuthorizationServerConfigProperties(IntrospectionMode.NONE, null, null, null));
        props.validate();
    }

    @ParameterizedTest
    @EnumSource(value = IntrospectionMode.class, names = {"NONE"}, mode = EnumSource.Mode.EXCLUDE)
    void loadIntrospectionProperties_IntrospectionPropertiesWithModeDifferentFromNone_ThrowException(IntrospectionMode introspectionMode) {
        ResourceServerProperties props = new ResourceServerProperties();
        setIntrospectionMode(props, IntrospectionMode.ALWAYS);
        props.setAuthorizationServer(getAuthorizationServerConfigProperties(introspectionMode, "client-id", "client-secret", "some-uri"));
        assertThatThrownBy(props::validate).hasMessageStartingWith(
                "Configuring an introspection mode other than 'NONE' is not supported on the authorization server level.");
    }

    @Test
    void loadIntrospectionProperties_IntrospectionPropertiesWithoutClientId_ThrowException() {
        ResourceServerProperties props = new ResourceServerProperties();
        setIntrospectionMode(props, IntrospectionMode.ALWAYS);
        props.setAuthorizationServer(getAuthorizationServerConfigProperties(null, null, "some-secret", null));
        assertThatThrownBy(props::validate).hasMessage("client-id must be provided");
    }

    @Test
    void loadIntrospectionProperties_IntrospectionPropertiesWithoutClientSecretId_ThrowException() {
        ResourceServerProperties props = new ResourceServerProperties();
        setIntrospectionMode(props, IntrospectionMode.ALWAYS);
        props.setAuthorizationServer(getAuthorizationServerConfigProperties(null, "some-client-id", null, null));
        assertThatThrownBy(props::validate).hasMessage("client-secret must be provided");
    }

    private static AuthorizationServerConfigProperties getAuthorizationServerConfigProperties(String introspectionUri) {
        return getAuthorizationServerConfigProperties(null, "myId", "mySecret", introspectionUri);
    }

    private static AuthorizationServerConfigProperties getAuthorizationServerConfigProperties(IntrospectionMode introspectionMode,
                                                                                              String clientId, String clientSecret,
                                                                                              String introspectionUri) {
        IntrospectionProperties introspectionProperties = new IntrospectionProperties();
        introspectionProperties.setMode(introspectionMode);
        introspectionProperties.setClientId(clientId);
        introspectionProperties.setClientSecret(clientSecret);
        introspectionProperties.setUri(introspectionUri);
        AuthorizationServerConfigProperties authorizationServerConfigProperties = new AuthorizationServerConfigProperties();
        authorizationServerConfigProperties.setIssuer("issuer");
        authorizationServerConfigProperties.setIntrospection(introspectionProperties);
        return authorizationServerConfigProperties;
    }

    private static void setIntrospectionMode(ResourceServerProperties props, IntrospectionMode introspectionMode) {
        IntrospectionResourceProperties introspectionResourceProperties = new IntrospectionResourceProperties();
        props.setIntrospection(introspectionResourceProperties);
        introspectionResourceProperties.setMode(introspectionMode);
    }

}
