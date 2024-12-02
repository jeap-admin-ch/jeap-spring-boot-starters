package ch.admin.bit.jeap.security.resource.log;

import ch.admin.bit.jeap.security.resource.configuration.JeapOauth2ResourceServerAuthenticationEntryPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.server.BearerTokenServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
public class LoggingBearerTokenServerAuthenticationEntryPoint implements JeapOauth2ResourceServerAuthenticationEntryPoint {

    private final BearerTokenServerAuthenticationEntryPoint bearerTokenServerAuthenticationEntryPoint = new BearerTokenServerAuthenticationEntryPoint();

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException authException) {
        return bearerTokenServerAuthenticationEntryPoint.commence(exchange,authException).
                doFirst(() -> log.info("Authentication failure on request path '{}': '{}'.", exchange.getRequest().getPath(), authException.getMessage()));
    }
}
