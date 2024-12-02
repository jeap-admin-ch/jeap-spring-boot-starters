package ch.admin.bit.jeap.monitor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;

public class HttpMethodFilteringServerWebExchangeMatcherTest {

    @Test
    void testFilterGet() {
        testHttpMethodFilteringServerWebExchangeMatcher(HttpMethodFilteringServerWebExchangeMatcher::filterGet,  (filteredMatcherMatches, requestHttpMethod) ->
                filteredMatcherMatches && (requestHttpMethod == HttpMethod.GET));
    }


    @Test
    void testFilterGetAndPost() {
        testHttpMethodFilteringServerWebExchangeMatcher(HttpMethodFilteringServerWebExchangeMatcher::filterGetAndPost, (filteredMatcherMatches, requestHttpMethod) ->
                filteredMatcherMatches && ((requestHttpMethod == HttpMethod.GET) || (requestHttpMethod == HttpMethod.POST)));
    }

    void testHttpMethodFilteringServerWebExchangeMatcher(Function<ServerWebExchangeMatcher, ServerWebExchangeMatcher> serverWebExchangeMatcherFilter, BiFunction<Boolean, HttpMethod, Boolean> expectation) {
        for (boolean filteredMatcherMatches : List.of(true, false)) {
            for (HttpMethod requestHttpMethod : HttpMethod.values()) {
                ServerWebExchangeMatcher filteredMatcher = mockServerWebExchangeMatcher(filteredMatcherMatches);
                ServerWebExchange serverWebExchange = mockServerWebExchangeRequest(requestHttpMethod);
                ServerWebExchangeMatcher filteringServerWebExchangeMatcher = serverWebExchangeMatcherFilter.apply(filteredMatcher);

                Mono<MatchResult> matchResult = filteringServerWebExchangeMatcher.matches(serverWebExchange);

                Assertions.assertEquals(expectation.apply(filteredMatcherMatches, requestHttpMethod), matchResult.block().isMatch());
            }
        }
    }

    ServerWebExchange mockServerWebExchangeRequest(HttpMethod method) {
        ServerHttpRequest serverHttpRequest = Mockito.mock(ServerHttpRequest.class);
        Mockito.when(serverHttpRequest.getMethod()).thenReturn(method);
        ServerWebExchange serverWebExchangeMock = Mockito.mock(ServerWebExchange.class);
        Mockito.when(serverWebExchangeMock.getRequest()).thenReturn(serverHttpRequest);
        return serverWebExchangeMock;
    }

    ServerWebExchangeMatcher mockServerWebExchangeMatcher(boolean matches) {
        ServerWebExchangeMatcher serverWebExchangeMatcherMock = Mockito.mock(ServerWebExchangeMatcher.class);
        Mockito.when(serverWebExchangeMatcherMock.matches(any(ServerWebExchange.class))).thenReturn(getMatchResult(matches));
        return serverWebExchangeMatcherMock;
    }

    private Mono<MatchResult> getMatchResult(boolean matches) {
        return matches ? MatchResult.match() : MatchResult.notMatch();
    }

}
