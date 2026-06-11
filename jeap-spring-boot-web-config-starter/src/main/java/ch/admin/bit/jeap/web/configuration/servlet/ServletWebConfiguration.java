package ch.admin.bit.jeap.web.configuration.servlet;

import ch.admin.bit.jeap.web.configuration.HeaderConfiguration;
import ch.admin.bit.jeap.web.configuration.HttpHeaderFilterPostProcessor;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.util.Optional;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ServletWebConfiguration {

    @Bean
    AddHeadersFilter httpResponseHeaderFilter(HeaderConfiguration config,
                                              Optional<HttpHeaderFilterPostProcessor> optionalPostProcessor) {
        HttpHeaderFilterPostProcessor postProcessor = optionalPostProcessor.orElse(HttpHeaderFilterPostProcessor.NO_OP);
        ServletHeaders servletHeaders = new ServletHeaders(postProcessor, config.getAdditionalContentSources(), config.getContentSecurityPolicy(), config.getFeaturePolicy());
        return new AddHeadersFilter(config, servletHeaders);
    }

    /**
     * See {@link org.springframework.web.filter.ShallowEtagHeaderFilter} for details. The Etag header avoids sending
     * a response when it is already cached on the client. As such, it saves on bandwidth but not on server-side
     * processing time. The instantiation of the filter bean can be disabled with the property jeap.web.headers.etag=false.
     * We created our own subclass of the {@link org.springframework.web.filter.ShallowEtagHeaderFilter} to exlcude
     * Server-Sent Events (SSE) specifically. Without it the SSEs were waiting infinitly until the response is complete,
     * which never happens because it's an infinite stream.
     */
    @Bean
    @ConditionalOnProperty(name = "jeap.web.headers.etag", matchIfMissing = true)
    ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
        return new SseAwareEtagHeaderFilter();
    }

    /**
     * Disables ETag content-caching for {@code FORWARD} and {@code INCLUDE} dispatches.
     * <p>
     * To compute an ETag, {@link ShallowEtagHeaderFilter} wraps the response in a buffering wrapper and only copies the
     * buffered body to the real response after the filter chain unwinds. This is incompatible with a {@code forward:} to
     * a static resource (e.g. Spring Boot's welcome-page handler forwarding {@code /} to {@code index.html}): the Servlet
     * container commits and finalizes the response when {@code RequestDispatcher.forward()} completes, i.e. before control
     * returns to the buffering filter. The filter then finds the response already committed, skips the ETag, and can no
     * longer flush the buffered body — the client receives an empty page.
     * <p>
     * This filter runs at the start of each forward/include dispatch (before the target handler writes) and calls
     * {@link ShallowEtagHeaderFilter#disableContentCaching(ServletRequest)}. That makes the response wrapper return the
     * raw output stream so the forwarded handler writes straight through, and makes the outer ETag filter skip itself,
     * avoiding a double write. Only forwarded/included responses are affected; they are served without an ETag, while all
     * regular (non-forwarded) requests keep their ETag unchanged. Trading the marginal ETag value on a forward for a
     * correct response body is the intended tradeoff.
     */
    @Bean
    @ConditionalOnProperty(name = "jeap.web.headers.etag", matchIfMissing = true)
    FilterRegistrationBean<Filter> disableEtagCachingOnForwardFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(
                (ServletRequest request, ServletResponse response, FilterChain chain) -> {
                    ShallowEtagHeaderFilter.disableContentCaching(request);
                    chain.doFilter(request, response);
                });
        registration.setName("disableEtagCachingOnForwardFilter");
        registration.setDispatcherTypes(DispatcherType.FORWARD, DispatcherType.INCLUDE);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE); // run first within the forward dispatch
        return registration;
    }
}
