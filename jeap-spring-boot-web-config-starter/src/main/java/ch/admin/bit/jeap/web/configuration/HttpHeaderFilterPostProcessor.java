package ch.admin.bit.jeap.web.configuration;

import java.util.Map;

/**
 * Allows for applications to customize the HTTP headers added by a jEAP header filter. Simply provide a bean
 * implementing this interface, which will be invoked for all requests matched by the jEAP header filter.
 */
public interface HttpHeaderFilterPostProcessor {

    HttpHeaderFilterPostProcessor NO_OP = new HttpHeaderFilterPostProcessor() {
    };

    default void postProcessHeaders(Map<String, String> headers, String method, String path) {
    }
}
