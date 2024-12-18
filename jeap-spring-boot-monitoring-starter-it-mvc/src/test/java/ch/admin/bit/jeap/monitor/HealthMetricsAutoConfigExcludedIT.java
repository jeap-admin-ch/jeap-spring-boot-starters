package ch.admin.bit.jeap.monitor;

import ch.admin.bit.jeap.monitor.metrics.health.HealthMetricsAutoConfig;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = HealthMetricsAutoConfigExcludedIT.TestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
class HealthMetricsAutoConfigExcludedIT {

    @LocalManagementPort
    int localManagementPort;

    @MockitoBean(name = "exampleHealthIndicator")
    HealthIndicator exampleHealthIndicator;

    @Test
    void itShouldNotInvokeHealthIndicatorIfHealthMetricsConfigIsExcluded() {
        RestAssured.given()
                .port(localManagementPort)
                .auth().basic("prometheus", "test")
                .get("/jme-management-test/actuator/prometheus")
                .then().assertThat()
                .statusCode(200);
        verify(exampleHealthIndicator, never()).getHealth(true);
    }

    @SpringBootApplication(exclude = HealthMetricsAutoConfig.class)
    public static class TestApp {
        @Bean(name = "exampleHealthIndicator")
        public HealthIndicator exampleHealthIndicator() {
            return () -> Health.up().build();
        }
    }
}
