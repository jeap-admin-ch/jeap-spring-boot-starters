package ch.admin.bit.jeap.starter.application;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = RelativeRedirectsEnvPostProcessorTest.TestApp.class, webEnvironment = RANDOM_PORT)
class RelativeRedirectsEnvPostProcessorTest {

    @LocalServerPort
    int localServerPort;

    @SpringBootApplication
    static class TestApp {
        @RestController
        static class TestController {
            @GetMapping("redirectMe")
            public ModelAndView redirect() {
                return new ModelAndView("redirect:/target");
            }
        }
    }

    @Test
    void verifyRedirectionLocationHeaderIsRelative() {
        request()
                .get("/redirectMe")
                .then().assertThat()
                .statusCode(302)
                .header("Location", "/target");
    }

    private RequestSpecification request() {
        return RestAssured.given()
                .redirects().follow(false)
                .port(localServerPort);
    }
}