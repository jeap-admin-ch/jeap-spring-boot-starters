package ch.admin.bit.jeap.monitor;

import ch.admin.bit.jeap.monitor.metrics.log.MetricsTestApp;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;

import static org.hamcrest.Matchers.containsString;

@SpringBootTest(classes = MetricsTestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
class AppNameMetricIT {

    @LocalManagementPort
    int localManagementPort;

    @Test
    void shouldExposeAppNameMetric() {
        RestAssured.given()
                .port(localManagementPort)
                .auth().basic("prometheus", "test")
                .get("/jme-management-test/actuator/prometheus")
                .then().assertThat()
                .statusCode(200)
                .body(containsString("jeap_spring_app{name=\"jme-monitor-test\""));
    }
}
