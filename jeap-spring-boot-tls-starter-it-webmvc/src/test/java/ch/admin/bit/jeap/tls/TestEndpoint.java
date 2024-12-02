package ch.admin.bit.jeap.tls;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestEndpoint {

    @GetMapping("/hello")
    String helloWorld() {
        return "Hello World!";
    }

}
