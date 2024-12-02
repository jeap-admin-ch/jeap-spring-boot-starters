package ch.admin.bit.jeap.monitor;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
class HttpMethodFilteringServerWebExchangeMatcher implements ServerWebExchangeMatcher {

    @NonNull
    private final Collection<HttpMethod> matchingMethods;

    @NonNull
    private final ServerWebExchangeMatcher serverWebExchangeMatcher;

    static HttpMethodFilteringServerWebExchangeMatcher filter(ServerWebExchangeMatcher serverWebExchangeMatcher, HttpMethod... matchingMethods) {
        return new HttpMethodFilteringServerWebExchangeMatcher(Arrays.asList(matchingMethods), serverWebExchangeMatcher);
    }

    static ServerWebExchangeMatcher filterGet(ServerWebExchangeMatcher serverWebExchangeMatcher) {
        return new HttpMethodFilteringServerWebExchangeMatcher(List.of(HttpMethod.GET), serverWebExchangeMatcher);
    }

    static ServerWebExchangeMatcher filterGetAndPost(ServerWebExchangeMatcher serverWebExchangeMatcher) {
        return new HttpMethodFilteringServerWebExchangeMatcher(List.of(HttpMethod.GET, HttpMethod.POST), serverWebExchangeMatcher);
    }

    @Override
    public Mono<MatchResult> matches(ServerWebExchange exchange) {
        if (isRequestMethodMatching(exchange)) {
            return serverWebExchangeMatcher.matches(exchange);
        }
        else {
            return MatchResult.notMatch();
        }
    }

    private boolean isRequestMethodMatching(ServerWebExchange exchange) {
        HttpMethod requestMethod = exchange.getRequest().getMethod();
        return matchingMethods.stream().anyMatch(m -> m.equals(requestMethod));
    }

}
