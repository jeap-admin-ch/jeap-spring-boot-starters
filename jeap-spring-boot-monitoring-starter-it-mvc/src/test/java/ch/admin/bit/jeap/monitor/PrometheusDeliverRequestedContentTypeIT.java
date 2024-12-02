package ch.admin.bit.jeap.monitor;

import ch.admin.bit.jeap.monitor.metrics.log.MetricsTestApp;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(classes = MetricsTestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
class PrometheusDeliverRequestedContentTypeIT {

    @LocalManagementPort
    int localManagementPort;

    @Test
    void shouldReturnPlaintextIfRequested() {
        RestAssured.given()
                .port(localManagementPort)
                .auth().basic("prometheus", "test")
                .headers("Accept", "text/plain")
                .get("/jme-management-test/actuator/prometheus")
                .then().assertThat()
                .statusCode(200)
                .headers("Content-Type", startsWith("text/plain"))
                .body(containsString("jeap_spring_app{name=\"jme-monitor-test\""));
    }

    @Test
    void shouldReturnOpenmetricsIfRequested() {
        RestAssured.given()
                .port(localManagementPort)
                .auth().basic("prometheus", "test")
                .headers("Accept", "application/openmetrics-text")
                .get("/jme-management-test/actuator/prometheus")
                .then().assertThat()
                .statusCode(200)
                .headers("Content-Type", startsWith("application/openmetrics-text"))
                .body(containsString("jeap_spring_app{name=\"jme-monitor-test\""));
    }
}