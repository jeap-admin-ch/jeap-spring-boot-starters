package ch.admin.bit.jeap.config.client;

import ch.admin.bit.jeap.config.client.test.TestApplication;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;


import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@ActiveProfiles(profiles = "local")
@SpringBootTest(classes = TestApplication.class)
class ClientConfigIT {

    private static final WireMockServer WIRE_MOCK = new WireMockServer(0); // random port

    @BeforeAll
    static void setup() {
        stubConfigServerResponse();
        WIRE_MOCK.start();
        String configServerMockUrl = "http://localhost:%s/config".formatted(WIRE_MOCK.port());
        System.setProperty("spring.cloud.config.uri", configServerMockUrl);
    }

    @AfterAll
    static void teardown() {
        WIRE_MOCK.stop();
        System.clearProperty("spring.cloud.config.uri");
    }

    @Autowired
    private Environment env;

    @Test
    void testReadApplicationConfigFileAndFetchConfigurationFromConfigServerSuccesfully() {
        assertThat(env.getProperty("jme.test.application")).isEqualTo("somevalue");
        assertThat(env.getProperty("local-property","none")).isEqualTo("local-from-config-server");
        assertThat(env.getProperty("general-property", "none")).isEqualTo("general-from-config-server");
    }

    private static void stubConfigServerResponse() {
        final String configPath = "/config/test-app/local";
        WIRE_MOCK.stubFor(get(configPath)
                        .willReturn(ok()
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("ConfigServerResponse.json"))
        );
        final String defaultPath = "/config/test-app/default";
        WIRE_MOCK.stubFor(get(defaultPath)
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("ConfigServerDefaultResponse.json"))
        );
    }

}
