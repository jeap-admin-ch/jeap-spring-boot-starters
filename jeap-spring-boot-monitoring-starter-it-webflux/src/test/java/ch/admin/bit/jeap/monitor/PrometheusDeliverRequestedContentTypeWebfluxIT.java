package ch.admin.bit.jeap.monitor;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;

import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
class PrometheusDeliverRequestedContentTypeWebfluxIT {

    @LocalManagementPort
    int localManagementPort;


    @Test
    void shouldReturnPlaintextIfRequested() {
        requestWithPrometheusRole()
                .headers("Accept", "text/plain")
                .get("/jme-management-test/actuator/prometheus")
                .then().assertThat()
                .statusCode(200)
                .headers("Content-Type", startsWith("text/plain"));
    }

    @Test
    void shouldReturnPlaintextIfRequestedOpenmetrics() {
        requestWithPrometheusRole()
                .headers("Accept", "application/openmetrics-text")
                .get("/jme-management-test/actuator/prometheus")
                .then().assertThat()
                .statusCode(200)
                .headers("Content-Type", startsWith("application/openmetrics-text"));
    }

    private RequestSpecification requestWithoutRoles() {
        return RestAssured.given()
                .port(localManagementPort);
    }

    private RequestSpecification requestWithPrometheusRole() {
        return requestWithoutRoles()
                .auth().basic("prometheus", "test");
    }
}
