package ch.admin.bit.jeap.security.resource.properties;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;

import java.util.Set;

public interface AuthorizationServerConfiguration {

    String getIssuer();

    String getJwkSetUri();

    String getClaimSetConverterName();

    Set<JeapAuthenticationContext> getAuthenticationContexts();

}
