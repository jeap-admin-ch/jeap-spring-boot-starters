package ch.admin.bit.jeap.monitor;

import ch.admin.bit.jeap.monitor.metrics.log.TimedComponent;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = HealthContributorPrometheusMetricsEnabledIT.TestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@TestPropertySource(properties = "jeap.health.metric.contributor-metrics.enabled=true")
class HealthContributorPrometheusMetricsEnabledIT {

    @LocalManagementPort
    int localManagementPort;

    @Autowired
    TimedComponent timedComponent;
    @Autowired
    MeterRegistry meterRegistry;

    @Test
    void shouldExposeMockDbHealthIndicatorMetric() {
        String metrics = RestAssured.given()
                .port(localManagementPort)
                .auth().basic("prometheus", "thisisthepasswordusedtoaccesstheprometheusendpointsforthejeapmonitoringstarter")
                .get("/jme-management-test/actuator/prometheus")
                .then().statusCode(200)
                .extract().asString();

        assertThat(metrics).contains("health_indicator_status{component=\"db\"} 1.0");
        assertThat(metrics).contains("health_indicator_status{component=\"ldap\"} 0.0");
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

    @SpringBootApplication
    public static class TestApp {
        @Bean(name = "db")
        public HealthIndicator dbHealthIndicator() {
            return () -> Health.up().withDetail("mockDb", "ok").build();
        }

        @Bean(name = "ldap")
        public HealthIndicator ldapHealthIndicator() {
            return () -> Health.down().withDetail("mockLdap", "not ok").build();
        }
    }
}
