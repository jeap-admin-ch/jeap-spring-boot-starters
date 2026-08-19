package ch.admin.bit.jeap.security.resource.introspection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
class DelegatingToSpringJeapTokenIntrospector implements JeapTokenIntrospector {

    private final SpringOpaqueTokenIntrospector tokenIntrospector;

    DelegatingToSpringJeapTokenIntrospector(JeapTokenIntrospectorConfiguration config) {
        RestOperations restOperations = createRestOperations(config);
        this.tokenIntrospector = new SpringOpaqueTokenIntrospector(config.introspectionUri(), restOperations);
    }

    public Map<String, Object> introspect(String token) throws OAuth2IntrospectionException, InvalidBearerTokenException {
        try {
            OAuth2AuthenticatedPrincipal introspectionResult = tokenIntrospector.introspect(token);
            return introspectionResult.getAttributes();
        } catch (BadOpaqueTokenException bte) {
            throw new JeapIntrospectionInvalidTokenException(bte);
        } catch (OAuth2IntrospectionException oie) {
           Throwable oieCause = oie.getCause();
            if (oieCause instanceof ResourceAccessException) {
               throw new JeapIntrospectionException("Accessing token introspection endpoint failed.", oie);
           } else {
                throw new JeapIntrospectionException("Token introspection failed: " + oie.getMessage(), oie);
            }
        } catch (Exception e) {
            throw new JeapIntrospectionException("Token introspection failed: " + e.getMessage(), e);
        }
    }

    private RestOperations createRestOperations(JeapTokenIntrospectorConfiguration config) {
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        // RFC 6749 section 2.3.1 requires the client credentials to be form-urlencoded before they are used as
        // basic auth username and password. This also allows for client ids containing colons, which basic auth
        // would otherwise not permit.
        return restTemplateBuilder
                .connectTimeout(config.connectTimeout())
                .readTimeout(config.readTimeout())
                .interceptors(new BasicAuthenticationInterceptor(
                        URLEncoder.encode(config.clientId(), StandardCharsets.UTF_8),
                        URLEncoder.encode(config.clientSecret(), StandardCharsets.UTF_8)))
                .build();
    }

}
