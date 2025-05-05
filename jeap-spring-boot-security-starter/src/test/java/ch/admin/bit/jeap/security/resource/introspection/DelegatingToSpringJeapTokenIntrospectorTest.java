package ch.admin.bit.jeap.security.resource.introspection;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
class DelegatingToSpringJeapTokenIntrospectorTest {

    private static final String CLIENT_ID = "test-client";
    private static final String CLIENT_SECRET = "test-secret";
    private static final String INTROSPECTION_PATH = "/oauth2/introspect";
    private static final String VALID_TOKEN = "valid-token";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final String UNREACHABLE_ISSUER = "http://10.255.255.1";
    private static final Duration INTROSPECTOR_TIMEOUT = Duration.ofSeconds(1);

    private String issuer;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        int port = wmRuntimeInfo.getHttpPort();
        issuer = "http://localhost:" + port;
    }

    @Test
    void introspect_withValidToken_shouldReturnAttributes() {
        // Setup a mock response for a valid token and create a token introspector
        stubFor(post(urlEqualTo(INTROSPECTION_PATH))
                .withBasicAuth(CLIENT_ID, CLIENT_SECRET)
                .withRequestBody(containing("token=" + VALID_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                    "active": true,
                                    "sub": "test-subject",
                                    "scope": "openid email"
                                }
                                """)
                )
        );
        JeapTokenIntrospector tokenIntrospector = new DelegatingToSpringJeapTokenIntrospector(createIntrospectorConfig(issuer));

        // Call the introspector
        Map<String, Object> attributes = tokenIntrospector.introspect(VALID_TOKEN);

        // Assert that a request to the token endpoint was made
        verify(postRequestedFor(urlEqualTo(INTROSPECTION_PATH)));

        // Verify the response
        assertThat(attributes).isNotNull();
        assertThat(attributes.get("active")).isEqualTo(true);
        assertThat(attributes.get("sub")).isEqualTo("test-subject");
        assertThat(attributes.get("scope")).isEqualTo(List.of("openid", "email"));
    }


    @Test
    void introspect_withInvalidToken_shouldThrowIntrospectionInvalidTokenException() {
        // Setup a mock response for an ivalid token and create a token introspector
        stubFor(post(urlEqualTo(INTROSPECTION_PATH))
                .withBasicAuth(CLIENT_ID, CLIENT_SECRET)
                .withRequestBody(containing("token=" + INVALID_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                    "active": false
                                }
                                """)
                )
        );
        JeapTokenIntrospector tokenIntrospector = new DelegatingToSpringJeapTokenIntrospector(createIntrospectorConfig(issuer));

        // Assert calling the introspector throws an invalid-token-exception
        assertThatThrownBy(() -> tokenIntrospector.introspect(INVALID_TOKEN))
                .isInstanceOf((JeapIntrospectionInvalidTokenException.class));

        // Assert that a request to the token endpoint was made
        verify(postRequestedFor(urlEqualTo(INTROSPECTION_PATH)));
    }


    @Test()
    @Timeout(5) // Timeout to prevent the test from hanging indefinitely if the introspection timeout does not work
    void introspect_withConnectTimeout_shouldThrowJeapIntrospectionException() {
        // Create a token introspector configured with an unreachable introspection uri
        JeapTokenIntrospectorConfiguration config = createIntrospectorConfig(UNREACHABLE_ISSUER);
        JeapTokenIntrospector tokenIntrospector = new DelegatingToSpringJeapTokenIntrospector(config);

        // Assert calling the introspector throws an exception
        assertThatThrownBy(() -> tokenIntrospector.introspect(VALID_TOKEN))
                .isInstanceOf((JeapIntrospectionException.class))
                .hasMessage("Accessing token introspection endpoint failed.");
    }

    @Test
    @Timeout(5) // Timeout to prevent the test from hanging indefinitely if the introspection timeout does not work
    void introspect_withReadTimeout_shouldThrowJeapIntrospectionException() {
        // Setup a *delayed* mock response for a valid token and create a token introspector with a short read timeout
        final int responseDelay = (int) INTROSPECTOR_TIMEOUT.plusSeconds(1).toMillis();
        stubFor(post(urlEqualTo(INTROSPECTION_PATH))
                .withBasicAuth(CLIENT_ID, CLIENT_SECRET)
                .withRequestBody(containing("token=" + VALID_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withFixedDelay(responseDelay) // Delay the response longer than the read timeout
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                { "active": true }
                                """)
                )
        );
        JeapTokenIntrospectorConfiguration config = createIntrospectorConfig(issuer);
        JeapTokenIntrospector tokenIntrospector = new DelegatingToSpringJeapTokenIntrospector(config);

        // Assert calling the introspector throws an exception
        assertThatThrownBy(() -> tokenIntrospector.introspect(VALID_TOKEN))
                .isInstanceOf((JeapIntrospectionException.class))
                .hasMessage("Accessing token introspection endpoint failed.");

        // Assert that a request to the token endpoint was made
        verify(postRequestedFor(urlEqualTo(INTROSPECTION_PATH)));
    }

    @Test
    void introspect_withServerError_shouldThrowJeapIntrospectionException() {
        // Setup an error mock response for a valid token and create a token introspector
        stubFor(post(urlEqualTo(INTROSPECTION_PATH))
                .withBasicAuth(CLIENT_ID, CLIENT_SECRET)
                .withRequestBody(containing("token=" + VALID_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                    { "error": "server_error"}
                                    """)
                )
        );
        JeapTokenIntrospector tokenIntrospector =  new DelegatingToSpringJeapTokenIntrospector(createIntrospectorConfig(issuer));

        // Assert calling the introspector throws an exception
        assertThatThrownBy(() -> tokenIntrospector.introspect(VALID_TOKEN))
                .isInstanceOf((JeapIntrospectionException.class))
                .hasMessageStartingWith("Token introspection failed: 500 Server Error");

        // Assert that a request to the token endpoint was made
        verify(postRequestedFor(urlEqualTo(INTROSPECTION_PATH)));
    }


    private JeapTokenIntrospectorConfiguration createIntrospectorConfig(String issuer) {
        return new JeapTokenIntrospectorConfiguration(
                issuer,
                issuer + INTROSPECTION_PATH,
                CLIENT_ID,
                CLIENT_SECRET,
                INTROSPECTOR_TIMEOUT,
                INTROSPECTOR_TIMEOUT
        );
    }

}
