package ch.admin.bit.jeap.security.resource.log;

import ch.admin.bit.jeap.security.resource.configuration.JeapOauth2ResourceServerAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.web.access.server.BearerTokenServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
public class LoggingBearerTokenServerAccessDeniedHandler implements JeapOauth2ResourceServerAccessDeniedHandler {

    private final BearerTokenServerAccessDeniedHandler bearerTokenServerAccessDeniedHandler = new BearerTokenServerAccessDeniedHandler();
    private final boolean debugEnabled;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException accessDeniedException) {
        if (!debugEnabled) {
            return bearerTokenServerAccessDeniedHandler.handle(exchange, accessDeniedException)
                   .doFirst(() -> log.info("Access denied to request path '{}': '{}'.", exchange.getRequest().getPath(), accessDeniedException.getMessage()));
        } else {
            return exchange.getPrincipal().doOnNext(principal ->
                    log.debug("Access denied to request path '{}': '{}'. Authentication: '{}'.",
                          exchange.getRequest().getPath(), accessDeniedException.getMessage(), AuthenticationLogInfo.from(principal)))
            .then(bearerTokenServerAccessDeniedHandler.handle(exchange, accessDeniedException));
        }
    }

}
