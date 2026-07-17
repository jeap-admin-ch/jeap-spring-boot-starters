package ch.admin.bit.jeap.security.test.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = WireMockSpringBootIntegrationTest.TestApplication.class)
@EnableWireMock
class WireMockSpringBootIntegrationTest {

    @InjectWireMock
    private WireMockServer wireMockServer;

    @Value("${wiremock.server.baseUrl}")
    private String wireMockBaseUrl;

    @Test
    void startsWireMockAndPublishesItsBaseUrl() {
        assertThat(wireMockServer.isRunning()).isTrue();
        assertThat(wireMockBaseUrl).isEqualTo(wireMockServer.baseUrl());
    }

    @SpringBootConfiguration
    static class TestApplication {
    }
}
