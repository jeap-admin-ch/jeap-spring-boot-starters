package ch.admin.bit.jeap.monitor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;

public class HttpMethodFilteringRequestMatcherTest {

    @Test
    void testFilterGet() {
        testHttpMethodFilteringRequestMatcher(HttpMethodFilteringRequestMatcher::filterGet,  (filteredMatcherMatches, requestHttpMethod) ->
                filteredMatcherMatches && (requestHttpMethod == HttpMethod.GET));
    }


    @Test
    void testFilterGetAndPost() {
        testHttpMethodFilteringRequestMatcher(HttpMethodFilteringRequestMatcher::filterGetAndPost, (filteredMatcherMatches, requestHttpMethod) ->
                filteredMatcherMatches && ((requestHttpMethod == HttpMethod.GET) || (requestHttpMethod == HttpMethod.POST)));
    }

    void testHttpMethodFilteringRequestMatcher(Function<RequestMatcher, RequestMatcher> requestMatcherFilter, BiFunction<Boolean, HttpMethod, Boolean> expectation) {
        for (boolean filteredMatcherMatches : List.of(true, false)) {
            for (HttpMethod requestHttpMethod : HttpMethod.values()) {
                RequestMatcher filteredMatcher = mockRequestMatcher(filteredMatcherMatches);
                HttpServletRequest request = mockHttpServletRequest(requestHttpMethod);
                RequestMatcher filteringRequestMatcher = requestMatcherFilter.apply(filteredMatcher);

                boolean matches = filteringRequestMatcher.matches(request);

                Assertions.assertEquals(expectation.apply(filteredMatcherMatches, requestHttpMethod), matches);
            }
        }
    }

    HttpServletRequest mockHttpServletRequest(HttpMethod method) {
        HttpServletRequest requestMock = Mockito.mock(HttpServletRequest.class);
        Mockito.when(requestMock.getMethod()).thenReturn(method.name());
        return requestMock;
    }

    RequestMatcher mockRequestMatcher(boolean matches) {
        RequestMatcher requestMatcherMock = Mockito.mock(RequestMatcher.class);
        Mockito.when(requestMatcherMock.matches(any(HttpServletRequest.class))).thenReturn(matches);
        Mockito.when(requestMatcherMock.matcher(any(HttpServletRequest.class))).thenReturn(
                matches ? RequestMatcher.MatchResult.match() : RequestMatcher.MatchResult.notMatch());
        return requestMatcherMock;
    }

}
