package ch.admin.bit.jeap.web.configuration.webflux;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping
    Mono<String> getRoot() {
        return Mono.just("OK");
    }

    @GetMapping("/resource")
    Mono<String> getResource() {
        return Mono.just("OK");
    }
}
