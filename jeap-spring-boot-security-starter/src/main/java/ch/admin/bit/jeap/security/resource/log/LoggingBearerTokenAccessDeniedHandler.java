package ch.admin.bit.jeap.security.resource.log;

import ch.admin.bit.jeap.security.resource.configuration.JeapOauth2ResourceAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RequiredArgsConstructor
@Slf4j
public class LoggingBearerTokenAccessDeniedHandler implements JeapOauth2ResourceAccessDeniedHandler {

    private final BearerTokenAccessDeniedHandler bearerTokenAccessDeniedHandler = new BearerTokenAccessDeniedHandler();
    private final boolean debugEnabled;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        if (!debugEnabled) {
            log.info("Access denied to request path '{}': '{}'.", request.getRequestURI(), accessDeniedException.getMessage());
        }
        else {
            log.debug("Access denied to request path '{}': '{}'. Authentication: '{}'.",
                    request.getRequestURI(), accessDeniedException.getMessage(), AuthenticationLogInfo.from(request.getUserPrincipal()));
        }
        bearerTokenAccessDeniedHandler.handle(request, response, accessDeniedException);
    }

}
