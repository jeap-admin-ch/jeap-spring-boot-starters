package ch.admin.bit.jeap.web.configuration.servlet;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/some-consumer-api")
public class TestCustomConsumerAPIController {

    @GetMapping
    String getRoot() {
        return "OK";
    }

    @GetMapping("/resource")
    String getResource() {
        return "OK";
    }
}
