package ch.admin.bit.jeap.config.client;

import ch.admin.bit.jeap.config.client.test.TestApplication;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@ActiveProfiles({"local", "nobootstrap"})
@SpringBootTest(classes = TestApplication.class)
class NoBootstrapContextRefresherFixIT {

    private static final WireMockServer WIRE_MOCK = new WireMockServer(0); // random port

    @Autowired
    private ContextRefresher contextRefresher;

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
    void test_WhenNoBootstrapContext_thenDynamicPropertySourceShouldBeClearedWhenSourceNoLongerPresent() {
        assertThat(env.getProperty("some-other-dynamic-property")).isEqualTo("stays-put");
        String beforeRefresh = env.getProperty("some-property");
        assertThat(beforeRefresh).isEqualTo("dynamic");

        contextRefresher.refresh();

        assertThat(env.getProperty("some-other-dynamic-property")).isEqualTo("stays-put");
        String afterRefresh = env.getProperty("some-property");
        assertThat(afterRefresh).isEqualTo("static");
    }

    private static void stubConfigServerResponse() {
        String defaultPath = "/config/test-app/default";
        WIRE_MOCK.stubFor(get(defaultPath)
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("ConfigServerDefaultResponse.json"))
        );

        String scenario = "clear dynamic property";
        String clearedState = "dynamic property cleared";
        String configPath = "/config/test-app/local,nobootstrap";
        WIRE_MOCK.stubFor(get(configPath)
                .inScenario(scenario)
                .whenScenarioStateIs(STARTED)
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("ConfigServerResponseOverridden.json"))
                .willSetStateTo(clearedState)
        );
        WIRE_MOCK.stubFor(get(configPath)
                .inScenario(scenario)
                .whenScenarioStateIs(clearedState)
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("ConfigServerResponseCleared.json")));
    }
}