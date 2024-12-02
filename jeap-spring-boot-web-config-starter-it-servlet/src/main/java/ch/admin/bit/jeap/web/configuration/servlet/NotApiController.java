package ch.admin.bit.jeap.web.configuration.servlet;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Callable;

@RestController
@RequestMapping("/notapi")
public class NotApiController {

    @GetMapping("/resource")
    String getResource() {
        return "OK";
    }

    @PostMapping("/resource")
    void postResource() {
        // empty, for testing post requests
    }

    @GetMapping("/async-resource")
    Callable<String> getAsyncResource() {
        return () -> "OK";
    }
}
