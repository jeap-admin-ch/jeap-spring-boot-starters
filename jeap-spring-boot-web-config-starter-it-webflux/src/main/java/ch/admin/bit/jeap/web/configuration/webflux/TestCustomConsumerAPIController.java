package ch.admin.bit.jeap.web.configuration.webflux;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/some-consumer-api")
public class TestCustomConsumerAPIController {

    @GetMapping
    Mono<String> getRoot() {
        return Mono.just("OK");
    }

    @GetMapping("/resource")
    Mono<String> getResource() {
        return Mono.just("OK");
    }
}
