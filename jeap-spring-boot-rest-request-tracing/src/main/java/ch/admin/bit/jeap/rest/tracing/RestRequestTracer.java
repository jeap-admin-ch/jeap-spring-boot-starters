package ch.admin.bit.jeap.rest.tracing;

import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestRequestTracer {
    private final TracerConfiguration tracerConfiguration;
    private final List<RestRequestListener> restRequestListeners;
    private final List<RestResponseListener> restResponseListeners;

    @Builder(buildMethodName = "emit", builderMethodName = "onRequestBuilder", builderClassName = "OnRequestBuilder")
    private void onRequest(@NonNull String requestUri, @NonNull String method, String protocol) {
        if (restRequestListeners.isEmpty() || restRequestListeners.stream().noneMatch(RestRequestListener::isRequestListenerActive)) {
            return;
        }

        RestRequestTrace restRequestTrace = RestRequestTrace.builder()
                .method(method)
                .requestUri(requestUri)
                .protocol(protocol)
                .build();

        for (RestRequestListener listener : restRequestListeners) {
            try {
                listener.onRequest(restRequestTrace);
            } catch (Exception ex) {
                log.warn("Exception in request listener", ex);
            }
        }
    }

    @Builder(buildMethodName = "emit", builderMethodName = "onResponseBuilder", builderClassName = "OnResponseBuilder")
    private void onResponse(String method, @NonNull String requestUri, String requestUriPattern, String user,
                            ZonedDateTime incomingTime, Integer statusCode, InetSocketAddress remoteAddr,
                            Map<String, List<String>> requestHeaders, Map<String, List<String>> responseHeaders,
                            @NonNull Map<String, Object> attributes) {
        if (restResponseListeners.isEmpty() || restResponseListeners.stream().noneMatch(RestResponseListener::isResponseListenerActive)) {
            return;
        }

        String callingApplicationName = Optional.ofNullable(requestHeaders.get(AddSenderSystemHeader.APPLICATION_NAME_HEADER))
                .map(Collection::stream)
                .flatMap(Stream::findFirst)
                .orElse(null);

        for (RestResponseListener listener : restResponseListeners) {
            try {
                RestResponseTrace restResponseTrace = RestResponseTrace.builder()
                        .method(method)
                        .requestUri(requestUri)
                        .requestUriPattern(requestUriPattern)
                        .statusCode(statusCode)
                        .caller(callingApplicationName)
                        .user(user)
                        .elapsedMs(ChronoUnit.MILLIS.between(incomingTime, ZonedDateTime.now()))
                        .remoteAddr(remoteAddr == null ? null : remoteAddr.toString())
                        .requestHeaders(filterHeader(requestHeaders))
                        .responseHeaders(filterHeader(responseHeaders))
                        .attributes(filterAttributes(attributes))
                        .build();

                listener.onResponse(restResponseTrace);
            } catch (Exception ex) {
                log.warn("Exception in request listener", ex);
            }
        }
    }

    @SuppressWarnings("findbugs:WMI_WRONG_MAP_ITERATOR")
    private Map<String, List<String>> filterHeader(Map<String, List<String>> headers) {
        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (tracerConfiguration.headerBlacklisted(entry.getKey())) {
                continue;
            }
            result.put(entry.getKey(), maskHeaderIfNeeded(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private List<String> maskHeaderIfNeeded(String headerName, List<String> headerValues) {
        if (tracerConfiguration.headersToBeMasked(headerName)) {
            return headerValues.stream()
                    .map(v -> "***")
                    .collect(Collectors.toList());
        }
        return headerValues;
    }

    private Map<String, String> filterAttributes(Map<String, Object> attributes) {
        return attributes.keySet().stream()
                .filter(tracerConfiguration::attributeWhitelisted)
                .collect(Collectors.toMap(Function.identity(), k -> attributes.get(k).toString()));
    }
}
