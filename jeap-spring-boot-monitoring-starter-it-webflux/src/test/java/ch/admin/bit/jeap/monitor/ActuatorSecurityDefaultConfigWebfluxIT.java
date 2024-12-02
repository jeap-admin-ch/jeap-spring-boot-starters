package ch.admin.bit.jeap.monitor;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
class ActuatorSecurityDefaultConfigWebfluxIT {

    @LocalManagementPort
    int localManagementPort;

    @Test
    void testInfoAndHealthEndpoints_public() {
        requestWithoutRoles().get("/jme-management-test/actuator/info")
                .then().assertThat()
                .statusCode(200);
        requestWithoutRoles().get("/jme-management-test/actuator/health")
                .then().assertThat()
                .statusCode(200)
                .body(equalTo("{\"status\":\"UP\",\"groups\":[\"liveness\",\"readiness\"]}"));
        requestWithoutRoles().get("/jme-management-test/actuator/health/readiness")
                .then().assertThat()
                .statusCode(200)
                .body(equalTo("{\"status\":\"UP\"}"));
    }

    @Test
    void testHealthEndpoint_showsDetailForActuatorRole() {
        requestWithoutRoles().auth()
                .preemptive().basic("actuator", "test")
                .get("/jme-management-test/actuator/health")
                .then().assertThat()
                .statusCode(200)
                .body(containsString("UP"), containsString("diskSpace"));
    }

    @Test
    void testDiscoveryEndpoint_accessibleOnlyForActuatorRole() {
        requestWithoutRoles()
                .get("/jme-management-test/actuator")
                .then().assertThat()
                .statusCode(401);
        requestWithActuatorRole()
                .get("/jme-management-test/actuator")
                .then().assertThat()
                .statusCode(200);
    }

    @Test
    void testPrometheusEndpoint_protected() {
        requestWithoutRoles().get("/jme-management-test/actuator/prometheus")
                .then().assertThat()
                .statusCode(401);
        requestWithActuatorRole()
                .get("/jme-management-test/actuator/prometheus")
                .then().assertThat()
                .statusCode(403);
    }

    @Test
    void testPrometheusEndpoint_accessibleForPrometheusRole() {
        requestWithPrometheusRole()
                .get("/jme-management-test/actuator/prometheus")
                .then().assertThat()
                .statusCode(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "beans",
            "configprops",
            "env",
            "logfile",
            "loggers",
            "metrics",
            "scheduledtasks"})
    void testSpringBootAdminEndpoints_notActive(String endpoint) {
        requestWithActuatorRole()
                .get("/jme-management-test/actuator/" + endpoint).then()
                .assertThat().statusCode(403);
    }

    private RequestSpecification requestWithoutRoles() {
        return RestAssured.given()
                .port(localManagementPort);
    }

    private RequestSpecification requestWithActuatorRole() {
        return requestWithoutRoles()
                .auth().basic("actuator", "test");
    }

    private RequestSpecification requestWithPrometheusRole() {
        return requestWithoutRoles()
                .auth().basic("prometheus", "test");
    }
}
