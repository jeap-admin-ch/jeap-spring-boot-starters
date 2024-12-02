package ch.admin.bit.jeap.web.configuration.webflux;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/notapi")
public class NotApiController {

    @GetMapping("/resource")
    Mono<String> getResource() {
        return Mono.just("OK");
    }

    @PostMapping("/resource")
    void postResource() {
        // empty, for testing post requests
    }
}
