package ch.admin.bit.jeap.log.syslog;

import ch.admin.bit.jeap.log.syslog.test.MockSslServer;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toMap;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "jeap.logging.logrelay.port=" + SyslogIntegrationTestBase.PORT,
        "jeap.logging.logrelay.host=localhost"},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class SyslogIntegrationTestBase {
    final static int PORT = 13322;
    private final static Pattern SYSLOG_LINE_PATTERN = Pattern.compile("<\\d\\d>... (\\d| )\\d \\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d .* doppler\\[\\d+]: (?<payload>.*)");

    static MockSslServer mockSslServer;

    @LocalServerPort
    int localSpringBootServerPort;

    @BeforeAll
    static void startMockServer() {
        String filePath = Objects.requireNonNull(TLSSyslogAppenderIT.class.getResource("/testkeys.jks")).getFile();
        System.setProperty("javax.net.ssl.trustStore", filePath);
        System.setProperty("javax.net.ssl.trustStorePassword", "secret");

        mockSslServer = new MockSslServer();
        mockSslServer.start(PORT);
    }

    @AfterEach
    void cleanConnectionCache() {
        TLSSyslogAppender.cleanConnectionCache();
    }

    @AfterAll
    static void stopMockServer() {
        mockSslServer.stop();
    }

    @SuppressWarnings("unchecked")
    Map<String, String> getMsg(Map<String, Object> logEntry) {
        return (Map<String, String>) logEntry.get("msg");
    }

    Map<String, Object> awaitAndAssertOneJsonLogEntryOnSyslogContaining(String loggedMessage) {
        await("Syslog line containing " + loggedMessage).atMost(Duration.ofSeconds(10))
                .until(() -> mockSslServer.getReceivedData().contains(loggedMessage));

        List<String> receivedLines = mockSslServer.getReceivedLines();
        List<String> matches = receivedLines.stream()
                .filter(line -> line.contains(loggedMessage))
                .toList();

        assertEquals(1, matches.size(), receivedLines::toString);
        String logLine = matches.get(0);
        Matcher matcher = SYSLOG_LINE_PATTERN.matcher(logLine);
        assertTrue(matcher.matches(), () -> "Logline does not match expected syslog prefix: " + logLine);
        String payload = matcher.group("payload");
        return JsonParserFactory.getJsonParser().parseMap(payload);
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

    static org.hamcrest.Matcher<String> containsCounterBiggerThanZero(String counter) {
        String doesNotStartWithZero = "[1-9]";
        return matchesRegex(Pattern.compile(".*" + counter + " " + doesNotStartWithZero + ".*", Pattern.DOTALL));
    }

    @SpringBootApplication
    static class TestApp {
    }
}
