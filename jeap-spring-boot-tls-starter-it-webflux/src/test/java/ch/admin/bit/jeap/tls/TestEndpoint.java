package ch.admin.bit.jeap.tls;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class TestEndpoint {

    @GetMapping("/hello")
    Mono<String> helloWorld() {
        return Mono.just("Hello World!");
    }

}
