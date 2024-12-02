package ch.admin.bit.jeap.web.configuration.webflux;

import ch.admin.bit.jeap.web.configuration.HeaderConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * A filter adding Security/Caching header mostly useful for static resources consumed by a browser. See
 * {@link HeaderConfiguration} for configuration properties and defaults.
 */
public class AddHeadersWebFilter implements WebFilter {
    private static final Logger LOG = LoggerFactory.getLogger(AddHeadersWebFilter.class);

    private final HeaderConfiguration config;
    private final WebfluxHeaders webfluxHeaders;

    public AddHeadersWebFilter(HeaderConfiguration config, WebfluxHeaders webfluxHeaders) {
        this.config = config;
        this.webfluxHeaders = webfluxHeaders;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        try {
            String method = exchange.getRequest().getMethod().name();
            if (addHeadersForHttpMethod(method)) {
                String path = exchange.getRequest().getPath().pathWithinApplication().value();
                if (config.accept(path)) {
                    webfluxHeaders.addHeaders(exchange.getResponse(), method, path);
                }
            }
        } catch (Exception ex) {
            LOG.warn("Failed to add security and caching headers to response", ex);
        }

        return chain.filter(exchange);
    }

    private boolean addHeadersForHttpMethod(String method) {
        return config.getHttpMethods().contains(method);
    }
}
