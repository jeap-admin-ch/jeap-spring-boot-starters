package ch.admin.bit.jeap.rest.tracing;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Logs requests and responses for WebFlux-Applications using technology neutral {@link RestRequestTracer}
 * <p>
 * It is similar to ServletRequestTracer just for WebFlux
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@RequiredArgsConstructor
class ReactiveRequestTracer implements WebFilter {
    private final RestRequestTracer restRequestTracer;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        ZonedDateTime incomingTime = ZonedDateTime.now();

        //We have to log also the request in the Mono to get the tracing ids
        return Mono.empty()
                .doOnTerminate(() -> logRequest(exchange.getRequest()))
                .then(chain.filter(exchange))
                .doOnTerminate(() -> logResponse(exchange, incomingTime));
    }

    private void logRequest(ServerHttpRequest request) {
        restRequestTracer.onRequestBuilder()
                .method(method(request))
                .requestUri(request.getURI().toASCIIString())
                .protocol("")
                .emit();
    }

    private void logResponse(ServerWebExchange exchange, ZonedDateTime incomingTime) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String user = (String) Optional.ofNullable(exchange.getAttribute(ReactiveStoreUserFilter.USERNAME_ATTRIBUTE))
                .filter(value -> value instanceof String)
                .orElse(null);

        HttpStatusCode httpStatusCode = response.getStatusCode();
        Integer statusCode = httpStatusCode == null ? null : httpStatusCode.value();

        Object bestMatchingPattern = exchange.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        restRequestTracer.onResponseBuilder()
                .method(method(request))
                .requestUri(request.getURI().toASCIIString())
                .requestUriPattern(bestMatchingPattern == null ? null : bestMatchingPattern.toString())
                .user(user)
                .incomingTime(incomingTime)
                .statusCode(statusCode)
                .remoteAddr(request.getRemoteAddress())
                .responseHeaders(response.getHeaders())
                .requestHeaders(request.getHeaders())
                .attributes(exchange.getAttributes())
                .emit();
    }

    private static String method(ServerHttpRequest request) {
        return request.getMethod() == null ? null : request.getMethod().toString();
    }
}
