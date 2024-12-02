package ch.admin.bit.jeap.security.resource.log;

import ch.admin.bit.jeap.security.resource.configuration.JeapOauth2ResourceAuthenticationEntryPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
public class LoggingBearerTokenAuthenticationEntryPoint implements JeapOauth2ResourceAuthenticationEntryPoint {

    private final BearerTokenAuthenticationEntryPoint bearerTokenAuthenticationEntryPoint = new BearerTokenAuthenticationEntryPoint();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        log.info("Authentication failure on request path '{}' : '{}'.", request.getRequestURI(), authException.getMessage());
        bearerTokenAuthenticationEntryPoint.commence(request, response, authException);
    }

}
