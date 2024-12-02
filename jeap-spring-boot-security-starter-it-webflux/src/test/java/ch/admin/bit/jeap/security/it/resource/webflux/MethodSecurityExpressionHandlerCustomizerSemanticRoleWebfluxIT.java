package ch.admin.bit.jeap.security.it.resource.webflux;

import ch.admin.bit.jeap.security.it.resource.AbstractSemanticRoleAuthorizationIT;
import ch.admin.bit.jeap.security.it.resource.JeapMethodSecurityHandlerCustomizerTestConfig;
import ch.admin.bit.jeap.security.resource.configuration.JeapMethodSecurityExpressionHandlerCustomizer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.mockito.ArgumentMatchers.any;

@Import(JeapMethodSecurityHandlerCustomizerTestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=9010"})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
public class MethodSecurityExpressionHandlerCustomizerSemanticRoleWebfluxIT extends AbstractSemanticRoleAuthorizationIT {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private JeapMethodSecurityExpressionHandlerCustomizer customizer;

    protected MethodSecurityExpressionHandlerCustomizerSemanticRoleWebfluxIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
        super(serverPort, context);
    }

    @Test
    protected void testGetAuth_whenCustomizerProvidedAndAuthorized_thenCustomizerCalledAndAccessGranted() {
        super.testGetAuth_whenWithUserRoleAuthRead_thenAccessGranted();
        Mockito.verify(customizer).customize(any());
    }

    @Test
    protected void testGetAuth_whenCustomizerProvidedAndUnauthorized_thenCustomizerCalledAndAccessDenied() {
        super.testGetAuth_whenOnlyWithUserRoleDifferentThanAuthRead_thenAccessDenied();
        Mockito.verify(customizer).customize(any());
    }

}
