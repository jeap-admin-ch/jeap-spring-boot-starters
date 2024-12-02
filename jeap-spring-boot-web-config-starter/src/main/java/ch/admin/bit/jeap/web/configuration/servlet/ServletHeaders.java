package ch.admin.bit.jeap.web.configuration.servlet;

import ch.admin.bit.jeap.web.configuration.AbstractHeaders;
import ch.admin.bit.jeap.web.configuration.HttpHeaderFilterPostProcessor;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Map;

public class ServletHeaders extends AbstractHeaders<HttpServletResponse> {

    /**
     * @param additionalContentSources If given (nullable), is added to the list of allowed connect/frame sources in the Content-Security-Policy header
     * @param contentSecurityPolicy
     */
    ServletHeaders(HttpHeaderFilterPostProcessor postProcessor, Collection<String> additionalContentSources, String contentSecurityPolicy) {
        super(postProcessor, additionalContentSources, contentSecurityPolicy);
    }

    @Override
    protected void setHeadersMapToResponse(HttpServletResponse response, Map<String, String> headers) {
        headers.forEach(response::setHeader);
    }
}
