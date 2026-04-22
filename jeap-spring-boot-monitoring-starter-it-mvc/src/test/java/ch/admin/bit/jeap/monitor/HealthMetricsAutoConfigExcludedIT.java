package ch.admin.bit.jeap.monitor;

import ch.admin.bit.jeap.monitor.metrics.health.HealthMetricsAutoConfig;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = HealthMetricsAutoConfigExcludedIT.TestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMetrics
class HealthMetricsAutoConfigExcludedIT {

    @LocalManagementPort
    int localManagementPort;

    @MockitoBean(name = "exampleHealthIndicator")
    HealthIndicator exampleHealthIndicator;

    @Test
    void itShouldNotInvokeHealthIndicatorIfHealthMetricsConfigIsExcluded() {
        RestAssured.given()
                .port(localManagementPort)
                .auth().basic("prometheus", "thisisthepasswordusedtoaccesstheprometheusendpointsforthejeapmonitoringstarter")
                .get("/jme-management-test/actuator/prometheus")
                .then().assertThat()
                .statusCode(200);
        verify(exampleHealthIndicator, never()).health(true);
    }

    @SpringBootApplication(exclude = HealthMetricsAutoConfig.class)
    public static class TestApp {
        @Bean(name = "exampleHealthIndicator")
        public HealthIndicator exampleHealthIndicator() {
            return () -> Health.up().build();
        }
    }
}
