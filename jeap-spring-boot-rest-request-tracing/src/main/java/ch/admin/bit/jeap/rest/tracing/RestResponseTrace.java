package ch.admin.bit.jeap.rest.tracing;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Builder
@Value
public class RestResponseTrace {
    String method;
    String requestUri;
    String requestUriPattern;
    Integer statusCode;
    String caller;
    String user;
    long elapsedMs;
    String remoteAddr;
    Map<String, List<String>> requestHeaders;
    Map<String, List<String>> responseHeaders;
    Map<String, String> attributes;
}
