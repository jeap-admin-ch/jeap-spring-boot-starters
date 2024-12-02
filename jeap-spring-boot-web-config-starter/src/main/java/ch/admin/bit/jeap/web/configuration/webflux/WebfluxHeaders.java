package ch.admin.bit.jeap.web.configuration.webflux;

import ch.admin.bit.jeap.web.configuration.AbstractHeaders;
import ch.admin.bit.jeap.web.configuration.HttpHeaderFilterPostProcessor;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.util.Collection;
import java.util.Map;

public class WebfluxHeaders extends AbstractHeaders<ServerHttpResponse> {

    /**
     * @param additionalContentSources If given (nullable), is added to the list of allowed sources in the Content-Security-Policy header
     * @param contentSecurityPolicy
     */
    WebfluxHeaders(HttpHeaderFilterPostProcessor postProcessor, Collection<String> additionalContentSources, String contentSecurityPolicy) {
        super(postProcessor, additionalContentSources, contentSecurityPolicy);
    }

    @Override
    protected void setHeadersMapToResponse(ServerHttpResponse serverHttpResponse, Map<String, String> headers) {
        headers.forEach(serverHttpResponse.getHeaders()::set);
    }
}
