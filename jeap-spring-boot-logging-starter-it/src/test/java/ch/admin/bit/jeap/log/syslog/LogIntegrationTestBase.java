package ch.admin.bit.jeap.log.syslog;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.junitpioneer.jupiter.StdOut;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.stream.Collectors.toMap;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class LogIntegrationTestBase {

    @LocalServerPort
    int localSpringBootServerPort;


    @SuppressWarnings("unchecked")
    Map<String, String> getMsg(Map<String, Object> logEntry) {
        return (Map<String, String>) logEntry.get("msg");
    }

    Map<String, String> awaitAndAssertOneJsonLogEntryOnStdOutContaining(String loggedMessage, StdOut stdOut) {
        String logLine = awaitAndAssertOneLogEntryOnStdOutContaining(loggedMessage, stdOut);
        return stringMap(JsonParserFactory.getJsonParser().parseMap(logLine));
    }

    String awaitAndAssertOneLogEntryOnStdOutContaining(String loggedMessage, StdOut stdOut) {
        await("Stdout line containing " + loggedMessage).atMost(Duration.ofSeconds(30))
                .until(() -> Arrays.stream(stdOut.capturedLines()).anyMatch(line -> line.contains(loggedMessage)));

        List<String> capturedLines = List.of(stdOut.capturedLines());
        List<String> matches = capturedLines.stream()
                .filter(line -> line.contains(loggedMessage))
                .toList();

        assertEquals(1, matches.size(), capturedLines::toString);
        return matches.get(0);
    }

    Map<String, String> awaitAndAssertAtLeastOneJsonLogEntryOnStdOutContaining(String loggedMessage, StdOut stdOut) {
        await("Stdout line containing " + loggedMessage).atMost(Duration.ofSeconds(30))
                .until(() -> Arrays.stream(stdOut.capturedLines()).anyMatch(line -> line.contains(loggedMessage)));

        List<String> capturedLines = List.of(stdOut.capturedLines());
        List<String> matches = capturedLines.stream()
                .filter(line -> line.contains(loggedMessage))
                .toList();

        assertFalse(matches.isEmpty(), capturedLines::toString);
        String logLine = matches.get(0);
        return stringMap(JsonParserFactory.getJsonParser().parseMap(logLine));
    }

    private Map<String, String> stringMap(Map<String, Object> map) {
        return map.entrySet().stream()
                .collect(toMap(Entry::getKey, e -> e.getValue().toString()));
    }

    RequestSpecification requestWithPrometheusRole() {
        return RestAssured.given()
                .port(localSpringBootServerPort)
                .auth().basic("prometheus", "test");
    }

    @SpringBootApplication
    static class TestApp {
    }
}
