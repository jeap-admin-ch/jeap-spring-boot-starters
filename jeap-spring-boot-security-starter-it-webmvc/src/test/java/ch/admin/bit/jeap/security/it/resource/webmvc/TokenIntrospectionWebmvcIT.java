package ch.admin.bit.jeap.security.it.resource.webmvc;

import ch.admin.bit.jeap.security.it.mockserver.OAuth2MockServer;
import ch.admin.bit.jeap.security.it.resource.AbstractTokenIntrospectionIT;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Runs the token introspection tests of {@link AbstractTokenIntrospectionIT} once with an explicitly configured
 * introspection client id and once with the introspection client id derived from the resource id. The test methods
 * live in the shared base class, the nested classes only provide the differently configured application contexts.
 */
class TokenIntrospectionWebmvcIT {

    /**
     * Introspection client id explicitly configured in the 'resource-introspection' profile.
     */
    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    class ExplicitIntrospectionClientId extends AbstractTokenIntrospectionIT {
        ExplicitIntrospectionClientId(@LocalServerPort int serverPort, @Value("${spring.application.name}") String context) {
            super(serverPort, context);
        }
    }

    /**
     * No introspection client id configured -> the resource id must be used as the introspection client id,
     * i.e. the resource id must match the client id expected by the OAuth2 mock server.
     */
    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
            "jeap.security.oauth2.resourceserver.authorization-server.introspection.client-id=",
            "jeap.security.oauth2.resourceserver.resource-id=" + OAuth2MockServer.CLIENT_ID})
    class IntrospectionClientIdDerivedFromResourceId extends AbstractTokenIntrospectionIT {
        IntrospectionClientIdDerivedFromResourceId(@LocalServerPort int serverPort, @Value("${spring.application.name}") String context) {
            super(serverPort, context);
        }
    }

}
