package ch.admin.bit.jeap.monitor;

import ch.admin.bit.jeap.monitor.metrics.log.MetricsTestApp;
import ch.admin.bit.jeap.monitor.metrics.log.TimedComponent;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(classes = MetricsTestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
class PrometheusIT {

    @LocalManagementPort
    int localManagementPort;

    @Autowired
    TimedComponent timedComponent;
    @Autowired
    MeterRegistry meterRegistry;

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

    @Test
    void testTimedAnnotationSupportEnabled() {
        timedComponent.timedMethod();

        Timer timer = meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getType() == Meter.Type.TIMER)
                .filter(meter -> meter.getId().getName().equals("timed_method"))
                .map(Timer.class::cast)
                .findFirst().orElseThrow();

        assertThat(timer.count()).isGreaterThan(0);
    }
}
