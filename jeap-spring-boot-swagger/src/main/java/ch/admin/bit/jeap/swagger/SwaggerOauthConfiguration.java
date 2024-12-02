package ch.admin.bit.jeap.swagger;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Configuration to user OAUTH in swagger UI. This is only loaded if this is a resource secured by jeap security
 * starter. It simply configures the selected authorization-server as OAUTH-Endpoint for swagger.
 */

@AutoConfiguration
@ConditionalOnProperty("jeap.security.oauth2.resourceserver.authorization-server.issuer")
@SecurityScheme(
        name = "OIDC",
        type = SecuritySchemeType.OPENIDCONNECT,
        description = "OAuth2-Authentication",
        openIdConnectUrl = "${jeap.swagger.oauth.openIdConnectUrl}",
        flows = @OAuthFlows(
                authorizationCode = @OAuthFlow(),
                clientCredentials = @OAuthFlow()
        )
)
public class SwaggerOauthConfiguration {
}
