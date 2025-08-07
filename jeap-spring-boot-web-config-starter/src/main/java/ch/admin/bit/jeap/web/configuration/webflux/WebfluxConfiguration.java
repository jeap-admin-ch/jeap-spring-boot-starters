package ch.admin.bit.jeap.web.configuration.webflux;

import ch.admin.bit.jeap.web.configuration.HeaderConfiguration;
import ch.admin.bit.jeap.web.configuration.HttpHeaderFilterPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class WebfluxConfiguration {

    @Bean
    AddHeadersWebFilter addResponseHeaderWebFilter(HeaderConfiguration config,
                                                   Optional<HttpHeaderFilterPostProcessor> optionalPostProcessor) {
        HttpHeaderFilterPostProcessor postProcessor = optionalPostProcessor.orElse(HttpHeaderFilterPostProcessor.NO_OP);
        WebfluxHeaders headers = new WebfluxHeaders(postProcessor, config.getAdditionalContentSources(), config.getContentSecurityPolicy(), config.getFeaturePolicy());
        return new AddHeadersWebFilter(config, headers);
    }
}
