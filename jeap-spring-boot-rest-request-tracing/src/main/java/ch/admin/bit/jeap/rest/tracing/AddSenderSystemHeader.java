package ch.admin.bit.jeap.rest.tracing;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Add a header with the name of the current service to each REST-Call
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(WebClient.Builder.class)
public class AddSenderSystemHeader implements WebClientCustomizer {
    public final static String APPLICATION_NAME_HEADER = "JEAP-APPLICATION-NAME";

    private final TracerConfiguration tracerConfiguration;

    @Override
    public void customize(WebClient.Builder webClientBuilder) {
        webClientBuilder.defaultHeader(APPLICATION_NAME_HEADER, tracerConfiguration.getApplicationName());
    }
}
