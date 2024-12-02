package ch.admin.bit.jeap.rest.tracing;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Add a header with the name of the current service to each REST-Call
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(RestClient.Builder.class)
public class AddSenderSystemHeaderToRestClient implements RestClientCustomizer {
    public static final String APPLICATION_NAME_HEADER = "JEAP-APPLICATION-NAME";

    private final TracerConfiguration tracerConfiguration;

    @Override
    public void customize(RestClient.Builder builder) {
        builder.defaultHeader(APPLICATION_NAME_HEADER, tracerConfiguration.getApplicationName());
    }
}
