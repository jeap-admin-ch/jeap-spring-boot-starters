package ch.admin.bit.jeap.rest.tracing;

import lombok.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Configuration for the REST-Tracer {@link RestRequestTracer}
 */
@Data
@AutoConfiguration
@PropertySource("classpath:tracer.properties")
@ConfigurationProperties("jeap.rest.tracing")
@ComponentScan
@NoArgsConstructor
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ConditionalOnWebApplication
public class TracerConfiguration {
    /**
     * A list of headers that shall not be logged
     * All headers matching any of this prefixes is ignored from logging
     */
    @Builder.Default
    private List<String> headerBlacklist = List.of();
    /**
     * A list of headers that must be masked, e.g. if they contain sensitive information
     * All headers matching any of this prefixes is logged only as ***
     */
    @Builder.Default
    private List<String> headerMasked = List.of();
    /**
     * A list of request-attributes to log. All attributes matching any of this prefixes are logged
     */
    @Builder.Default
    private List<String> attributesWhitelist = List.of();

    /**
     * Name of the current application, to be added as Header. By default ${spring.application.name}
     */
    private String applicationName;

    private Pattern uriFilterPattern;

    private boolean fullResponseDetailsInMessage;

    public boolean headerBlacklisted(String headerName) {
        String headerNameUpperCase = headerName.toUpperCase();
        return headerBlacklist.stream()
                .map(String::toUpperCase)
                .anyMatch(headerNameUpperCase::startsWith);
    }

    public boolean headersToBeMasked(String headerName) {
        String headerNameUpperCase = headerName.toUpperCase();
        return headerMasked.stream()
                .map(String::toUpperCase)
                .anyMatch(headerNameUpperCase::startsWith);
    }

    public boolean attributeWhitelisted(String attributeName) {
        String attributeNameUpperCase = attributeName.toUpperCase();
        return attributesWhitelist.stream()
                .map(String::toUpperCase)
                .anyMatch(attributeNameUpperCase::startsWith);
    }
}
