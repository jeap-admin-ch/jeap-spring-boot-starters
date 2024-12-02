package ch.admin.bit.jeap.log.rest;

import ch.admin.bit.jeap.rest.tracing.*;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static net.logstash.logback.argument.StructuredArguments.value;

// For backwards compatibility, the pre-refactoring topic name is retained
@Slf4j(topic = "ch.admin.bit.jeap.log.RestRequestTracer")
public class RestRequestLogger implements RestRequestListener, RestResponseListener {

    private static final String REDUCED_MESSAGE_FORMAT_FOR_JSON_LOGS = "{} {} {} {} {}";

    private final Pattern filterPattern;
    private final boolean fullResponseDetailsInMessage;

    public RestRequestLogger(TracerConfiguration tracerConfiguration) {
        this.filterPattern = tracerConfiguration.getUriFilterPattern();
        this.fullResponseDetailsInMessage = tracerConfiguration.isFullResponseDetailsInMessage();
    }

    @Override
    public void onRequest(RestRequestTrace restRequestTrace) {
        String uri = restRequestTrace.getRequestUri();
        if (isRequestListenerActive() && shouldLogUri(uri)) {
            log.trace("Incoming {} {} Request to {}",
                    restRequestTrace.getProtocol(),
                    value("method", restRequestTrace.getMethod()),
                    value("uri", uri));
        }
    }

    @Override
    public void onResponse(RestResponseTrace restResponseTrace) {
        String uri = restResponseTrace.getRequestUri();
        if (isResponseListenerActive() && shouldLogUri(uri)) {
            String format = fullResponseDetailsInMessage ?
                    "{} {} {} {} {} {} {} {} {} {}" : REDUCED_MESSAGE_FORMAT_FOR_JSON_LOGS;
            log.debug(format,
                    value("method", restResponseTrace.getMethod()),
                    value("uri", uri),
                    keyValue("result", restResponseTrace.getStatusCode()),
                    keyValue("caller", restResponseTrace.getCaller()),
                    keyValue("user", restResponseTrace.getUser()),
                    keyValue("dt", restResponseTrace.getElapsedMs()),
                    keyValue("remoteAddr", restResponseTrace.getRemoteAddr()),
                    keyValue("requestHeaders", restResponseTrace.getRequestHeaders()),
                    keyValue("responseHeaders", restResponseTrace.getResponseHeaders()),
                    keyValue("attributes", restResponseTrace.getAttributes()));
        }
    }

    private boolean shouldLogUri(String uri) {
        if (filterPattern == null) {
            return true;
        }

        return !filterPattern.matcher(uri).matches();
    }

    @Override
    public boolean isRequestListenerActive() {
        return log.isTraceEnabled();
    }

    @Override
    public boolean isResponseListenerActive() {
        return log.isDebugEnabled();
    }
}
