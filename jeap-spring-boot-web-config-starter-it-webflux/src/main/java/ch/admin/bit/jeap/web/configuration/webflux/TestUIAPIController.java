package ch.admin.bit.jeap.web.configuration.webflux;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ui-api")
public class TestUIAPIController {

    @GetMapping
    Mono<String> getRoot() {
        return Mono.just("OK");
    }

    @GetMapping("/resource")
    Mono<String> getResource() {
        return Mono.just("OK");
    }
}
