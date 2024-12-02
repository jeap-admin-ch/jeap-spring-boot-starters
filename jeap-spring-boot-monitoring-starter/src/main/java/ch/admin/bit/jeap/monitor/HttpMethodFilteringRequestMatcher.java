package ch.admin.bit.jeap.monitor;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
class HttpMethodFilteringRequestMatcher implements RequestMatcher {

    @NonNull
    private final Collection<HttpMethod> matchingMethods;

    @NonNull
    private final RequestMatcher requestMatcher;

    static HttpMethodFilteringRequestMatcher filter(RequestMatcher requestMatcher, HttpMethod... matchingMethods) {
        return new HttpMethodFilteringRequestMatcher(Arrays.asList(matchingMethods), requestMatcher);
    }

    static RequestMatcher filterGet(RequestMatcher requestMatcher) {
        return new HttpMethodFilteringRequestMatcher(List.of(HttpMethod.GET), requestMatcher);
    }

    static RequestMatcher filterGetAndPost(RequestMatcher requestMatcher) {
        return new HttpMethodFilteringRequestMatcher(List.of(HttpMethod.GET, HttpMethod.POST), requestMatcher);
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return isRequestMethodMatching(request) && requestMatcher.matches(request);
    }

    private boolean isRequestMethodMatching(HttpServletRequest request) {
        String requestMethod = request.getMethod();
        return matchingMethods.stream().anyMatch(m -> m.matches(requestMethod));
    }

}
