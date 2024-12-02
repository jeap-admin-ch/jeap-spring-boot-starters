package ch.admin.bit.jeap.web.configuration.servlet;

import ch.admin.bit.jeap.web.configuration.HeaderConfiguration;
import ch.admin.bit.jeap.web.configuration.HttpHeaderFilterPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.util.Optional;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ServletWebConfiguration {

    @Bean
    AddHeadersFilter httpResponseHeaderFilter(HeaderConfiguration config,
                                              Optional<HttpHeaderFilterPostProcessor> optionalPostProcessor) {
        HttpHeaderFilterPostProcessor postProcessor = optionalPostProcessor.orElse(HttpHeaderFilterPostProcessor.NO_OP);
        ServletHeaders servletHeaders = new ServletHeaders(postProcessor, config.getAdditionalContentSources(), config.getContentSecurityPolicy());
        return new AddHeadersFilter(config, servletHeaders);
    }

    /**
     * See {@link org.springframework.web.filter.ShallowEtagHeaderFilter} for details. The ETag header avoids sending
     * a response when it is already cached on the client. As such, it saves on bandwith but not on server-side
     * processing time. The instantiation of the filter bean can be disabled with the property jeap.web.headers.etag=false.
     */
    @Bean
    @ConditionalOnProperty(name = "jeap.web.headers.etag", matchIfMissing = true)
    ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
        return new ShallowEtagHeaderFilter();
    }
}
