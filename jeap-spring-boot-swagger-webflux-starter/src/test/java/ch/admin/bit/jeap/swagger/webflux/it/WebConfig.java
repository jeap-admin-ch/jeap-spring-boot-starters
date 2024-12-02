package ch.admin.bit.jeap.swagger.webflux.it;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.WebFilter;

@Configuration
public class WebConfig {
    @Bean
    @ConditionalOnProperty("spring.webflux.base-path")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter contextPathWebFilter(WebFluxProperties props) {
        final String basePath = props.getBasePath();

        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String requestPath = request.getURI().getPath();
            if (requestPath.startsWith(basePath + "/") || requestPath.equals(basePath)) {
                return chain.filter(exchange.mutate().request(request.mutate().contextPath(basePath).build()).build());
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        };
    }
}
