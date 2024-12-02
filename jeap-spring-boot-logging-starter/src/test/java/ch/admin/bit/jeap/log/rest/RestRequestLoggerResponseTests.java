package ch.admin.bit.jeap.log.rest;

import ch.admin.bit.jeap.rest.tracing.AddSenderSystemHeader;
import ch.admin.bit.jeap.rest.tracing.RestRequestTracer;
import ch.admin.bit.jeap.rest.tracing.TracerConfiguration;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RestRequestLoggerResponseTests {
    private final TracerConfiguration config = TracerConfiguration.builder()
            .attributesWhitelist(List.of("TEST"))
            .headerBlacklist(List.of("X-BLACKLIST"))
            .headerMasked(List.of("X-MASKED"))
            .uriFilterPattern(Pattern.compile(".*/actuator/.*"))
            .build();
    private final RestRequestLogger logger = new RestRequestLogger(config);
    private final RestRequestTracer target = new RestRequestTracer(config, List.of(logger), List.of(logger));

    @Test
    void testJsonOutput() {
        target.onResponseBuilder()
                .method("POST")
                .user(null)
                .requestUri("https://example.com/test?a=b")
                .requestUriPattern("pattern")
                .incomingTime(ZonedDateTime.now())
                .statusCode(202)
                .remoteAddr(null)
                .requestHeaders(Collections.emptyMap())
                .responseHeaders(Collections.emptyMap())
                .attributes(Collections.emptyMap())
                .emit();
        TestHelper.assertEqualsExceptTimeAndUnresolvedHostname("json_response.log", "json.log");
    }

    @Test
    void testJsonOutput_expectActuatorRequestsToBeFiltered() {
        target.onResponseBuilder()
                .method("POST")
                .user(null)
                .requestUri("https://example.com/test?a=b")
                .incomingTime(ZonedDateTime.now())
                .statusCode(202)
                .remoteAddr(null)
                .requestHeaders(Collections.emptyMap())
                .responseHeaders(Collections.emptyMap())
                .attributes(Collections.emptyMap())
                .emit();
        target.onResponseBuilder()
                .method("POST")
                .user(null)
                .requestUri("https://example.com/actuator/health")
                .requestUriPattern("/actuator/health")
                .incomingTime(ZonedDateTime.now())
                .statusCode(200)
                .remoteAddr(null)
                .requestHeaders(Collections.emptyMap())
                .responseHeaders(Collections.emptyMap())
                .attributes(Collections.emptyMap())
                .emit();
        target.onResponseBuilder()
                .method("POST")
                .user(null)
                .requestUri("https://example.com/actuator/health")
                .requestUriPattern(null)
                .incomingTime(ZonedDateTime.now())
                .statusCode(200)
                .remoteAddr(null)
                .requestHeaders(Collections.emptyMap())
                .responseHeaders(Collections.emptyMap())
                .attributes(Collections.emptyMap())
                .emit();
        TestHelper.assertEqualsExceptTimeAndUnresolvedHostname("json_response.log", "json.log");
    }

    @Test
    void testClassicOutput() {
        target.onResponseBuilder()
                .method("POST")
                .user(null)
                .requestUri("https://example.com/test?a=b")
                .incomingTime(ZonedDateTime.now())
                .statusCode(202)
                .remoteAddr(null)
                .requestHeaders(Collections.emptyMap())
                .responseHeaders(Collections.emptyMap())
                .attributes(Collections.emptyMap())
                .emit();
        TestHelper.assertEqualsExceptTimeAndUnresolvedHostname("classic_response.log", "classic.log");
    }

    @Test
    void testClassicOutput_withFullResponseDetailsInMessage() {
        TracerConfiguration config = TracerConfiguration.builder()
                .fullResponseDetailsInMessage(true)
                .build();
        RestRequestLogger logger = new RestRequestLogger(config);
        RestRequestTracer tracer = new RestRequestTracer(config, List.of(logger), List.of(logger));
        Map<String, List<String>> headers = Map.of(
                "req", List.of("value"),
                AddSenderSystemHeader.APPLICATION_NAME_HEADER, List.of("calling-app"));
        tracer.onResponseBuilder()
                .method("POST")
                .user("user")
                .requestUri("https://example.com/test?a=b")
                .incomingTime(ZonedDateTime.now())
                .statusCode(202)
                .remoteAddr(InetSocketAddress.createUnresolved("localhost", 123))
                .requestHeaders(headers)
                .responseHeaders(Map.of("resp", List.of("value")))
                .attributes(Map.of("attr", "value"))
                .emit();
        TestHelper.assertEqualsExceptTimeAndUnresolvedHostname("classic_response_full_message_details.log", "classic.log");
    }

    @Test
    void testNullValuesDoesNotThrow() {
        assertDoesNotThrow(() ->
                target.onResponseBuilder()
                        .method(null)
                        .user(null)
                        .requestUri("https://example.com/test?a=b")
                        .incomingTime(ZonedDateTime.now())
                        .statusCode(202)
                        .remoteAddr(null)
                        .requestHeaders(Collections.emptyMap())
                        .responseHeaders(Collections.emptyMap())
                        .attributes(Collections.emptyMap())
                        .emit());
    }

    @Test
    void testUser() {
        target.onResponseBuilder()
                .method("POST")
                .user("hans")
                .requestUri("https://example.com/test?a=b")
                .incomingTime(ZonedDateTime.now())
                .statusCode(202)
                .remoteAddr(null)
                .requestHeaders(Collections.emptyMap())
                .responseHeaders(Collections.emptyMap())
                .attributes(Collections.emptyMap())
                .emit();
        TestHelper.assertEqualsExceptTimeAndUnresolvedHostname("classic_response_user.log", "classic.log");
        TestHelper.assertEqualsExceptTimeAndUnresolvedHostname("json_response_user.log", "json.log");
    }


    @Test
    void testApplication() {
        target.onResponseBuilder()
                .method("POST")
                .user(null)
                .requestUri("https://example.com/test?a=b")
                .incomingTime(ZonedDateTime.now())
                .statusCode(202)
                .remoteAddr(null)
                .requestHeaders(Map.of(AddSenderSystemHeader.APPLICATION_NAME_HEADER, List.of("APP_NAME")))
                .responseHeaders(Collections.emptyMap())
                .attributes(Collections.emptyMap())
                .emit();
        TestHelper.assertEqualsExceptTimeAndUnresolvedHostname("classic_response_application.log", "classic.log");
        TestHelper.assertEqualsExceptTimeAndUnresolvedHostname("json_response_application.log", "json.log");
    }

    @Test
    void testHeaders() {
        target.onResponseBuilder()
                .method("POST")
                .user(null)
                .requestUri("https://example.com/test?a=b")
                .incomingTime(ZonedDateTime.now())
                .statusCode(202)
                .remoteAddr(null)
                .requestHeaders(Map.of("X-Tst", List.of("v1", "v2")))
                .responseHeaders(Map.of("X-Tst2", List.of("v4", "v5")))
                .attributes(Collections.emptyMap())
                .emit();
        TestHelper.assertEqualsExceptTimeAndUnresolvedHostname("json_response_header.log", "json.log");
    }

    @Test
    void testHeadersBacklist() {
        target.onResponseBuilder()
                .method("POST")
                .user(null)
                .requestUri("https://example.com/test?a=b")
                .incomingTime(ZonedDateTime.now())
                .statusCode(202)
                .remoteAddr(null)
                .requestHeaders(Collections.emptyMap())
                .responseHeaders(Collections.emptyMap())
                .attributes(Collections.emptyMap())
                .emit();
        TestHelper.assertEqualsExceptTimeAndUnresolvedHostname("json_response.log", "json.log");
    }

    @Test
    void testHeadersMasked() {
        target.onResponseBuilder()
                .method("POST")
                .user(null)
                .requestUri("https://example.com/test?a=b")
                .incomingTime(ZonedDateTime.now())
                .statusCode(202)
                .remoteAddr(null)
                .requestHeaders(Map.of("X-Masked", List.of("v1", "v2")))
                .responseHeaders(Collections.emptyMap())
                .attributes(Collections.emptyMap())
                .emit();
        TestHelper.assertEqualsExceptTimeAndUnresolvedHostname("json_response_masked.log", "json.log");
    }

    @Test
    void testAttributeBacklist() {
        target.onResponseBuilder()
                .method("POST")
                .user(null)
                .requestUri("https://example.com/test?a=b")
                .incomingTime(ZonedDateTime.now())
                .statusCode(202)
                .remoteAddr(null)
                .requestHeaders(Collections.emptyMap())
                .responseHeaders(Collections.emptyMap())
                .attributes(Map.of("BLACKLIST", "This is an example"))
                .emit();
        TestHelper.assertEqualsExceptTimeAndUnresolvedHostname("json_response.log", "json.log");
    }

    @Test
    void testAttributes() {
        target.onResponseBuilder()
                .method("POST")
                .user(null)
                .requestUri("https://example.com/test?a=b")
                .incomingTime(ZonedDateTime.now())
                .statusCode(202)
                .remoteAddr(null)
                .requestHeaders(Collections.emptyMap())
                .responseHeaders(Collections.emptyMap())
                .attributes(Map.of("TEST", "This is an example"))
                .emit();
        TestHelper.assertEqualsExceptTimeAndUnresolvedHostname("json_response_attribute.log", "json.log");
    }


    @Test
    void testRemote() {
        target.onResponseBuilder()
                .method("POST")
                .user(null)
                .requestUri("https://example.com/test?a=b")
                .incomingTime(ZonedDateTime.now())
                .statusCode(202)
                .remoteAddr(InetSocketAddress.createUnresolved("localhost", 1))
                .requestHeaders(Collections.emptyMap())
                .responseHeaders(Collections.emptyMap())
                .attributes(Collections.emptyMap())
                .emit();
        TestHelper.assertEqualsExceptTimeAndUnresolvedHostname("json_response_remote.log", "json.log");
    }
}
