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


import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@ActiveProfiles({"local", "bootstrap"})
@SpringBootTest(classes = TestApplication.class)
class LegacyContextRefresherFixIT {

    private static final WireMockServer WIRE_MOCK = new WireMockServer(0); // random port

    @BeforeAll
    static void setup() {
        System.setProperty("spring.cloud.bootstrap.enabled", "true");
        stubScenarioPropertyOverriddenToCleared();
        WIRE_MOCK.start();
        String configServerMockUrl = "http://localhost:%s/config".formatted(WIRE_MOCK.port());
        System.setProperty("spring.cloud.config.uri", configServerMockUrl);
    }

    @AfterAll
    static void teardown() {
        WIRE_MOCK.stop();
        System.clearProperty("spring.cloud.bootstrap.enabled");
        System.clearProperty("spring.cloud.config.uri");
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private ContextRefresher contextRefresher;

    @Autowired
    private Environment env;

    @Test
    void testIt() {
        final String beforeRefresh = env.getProperty("some-property");
        assertThat(beforeRefresh).isEqualTo("dynamic");
        assertThat(env.getProperty("some-other-dynamic-property")).isEqualTo("stays-put");

        contextRefresher.refresh();

        final String afterRefresh = env.getProperty("some-property");
        assertThat(afterRefresh).isEqualTo("static");
        assertThat(env.getProperty("some-other-dynamic-property")).isEqualTo("stays-put");
    }

    private static void stubScenarioPropertyOverriddenToCleared() {
        final String scenario = "clear dynamic property";
        final String clearedState = "dynamic property cleared";
        final String configPath = "/config/test-app/local,bootstrap";
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
