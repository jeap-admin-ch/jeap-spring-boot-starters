package ch.admin.bit.jeap.web.configuration.servlet;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * Extends {@link ShallowEtagHeaderFilter} to skip ETag buffering for SSE (Server-Sent Events)
 * endpoints. The standard filter buffers the entire response to compute an ETag hash, which
 * prevents SSE events from being flushed to the client.
 */
public class SseAwareEtagHeaderFilter extends ShallowEtagHeaderFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

}
