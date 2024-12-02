package ch.admin.bit.jeap.swagger.webmvc.it;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition
public class SwaggerConfig {
    @Bean
    GroupedOpenApi externalApi() {
        return GroupedOpenApi.builder()
                .group("test")
                .pathsToMatch("/**")
                .build();
    }

}
