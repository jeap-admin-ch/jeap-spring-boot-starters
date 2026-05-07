package ch.admin.bit.jeap.log.rest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTracing
@SpringBootTest(
        classes = UnhandledExceptionLoggingFilterIT.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jeap.logging.rest.unhandled-exception-logging.enabled=true",
                "management.tracing.sampling.probability=1.0"
        })
class UnhandledExceptionLoggingFilterIT {

    @SpringBootApplication
    @RestController
    static class TestApp {
        @GetMapping("/boom")
        String boom() {
            throw new RuntimeException("kaboom");
        }
    }

    @LocalServerPort
    private int port;

    @SuppressWarnings("LoggerInitializedWithForeignClass")
    private final Logger filterLogger =
            (Logger) LoggerFactory.getLogger(UnhandledExceptionLoggingFilter.class);
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void attachAppender() {
        logAppender = new ListAppender<>();
        logAppender.start();
        filterLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachAppender() {
        filterLogger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    void unhandledControllerExceptionIsLoggedWithTraceIdInMdc() {
        RestAssured.given().port(port).get("/boom");

        assertThat(logAppender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getMDCPropertyMap())
                    .extractingByKey("traceId")
                    .asString()
                    .matches("[0-9a-f]{32}");
        });
    }
}
