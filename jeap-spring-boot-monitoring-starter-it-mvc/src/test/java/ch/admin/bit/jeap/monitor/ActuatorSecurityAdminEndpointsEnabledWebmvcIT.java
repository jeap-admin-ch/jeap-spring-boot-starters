package ch.admin.bit.jeap.monitor;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpMethod;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(
        classes = TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {MonitoringConfig.ENABLE_ADMIN_ENDPOINTS_PROPERTY + "=true",
                      MonitoringConfig.ADDITIONAL_PERMITTED_ENDPOINTS_PROPERTY +
                        "=org.springframework.boot.actuate.cache.CachesEndpoint," +
                        "org.springframework.boot.actuate.web.mappings.MappingsEndpoint"})
@AutoConfigureObservability
class ActuatorSecurityAdminEndpointsEnabledWebmvcIT {

    @LocalManagementPort
    int localManagementPort;

    @ParameterizedTest
    @ValueSource(strings = {
            "beans",
            "configprops",
            "env",
            "loggers",
            "metrics",
            "scheduledtasks"})
    void testSpringBootAdminEndpoints_accessibleForActuatorRole(String endpoint) {
        requestWithActuatorRole()
                .get("/jme-management-test/actuator/" + endpoint).then()
                .assertThat().statusCode(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {"caches", "mappings"})
    void testAdditionalPermittedEndpoints_accessibleForActuatorRole(String additionalPermittedEndpoint) {
        requestWithActuatorRole()
                .get("/jme-management-test/actuator/" + additionalPermittedEndpoint).then()
                .assertThat().statusCode(200);
    }

    @Test
    void testLoggersEndpoint_accessibleToSetLogLevel() {
        requestWithActuatorRole()
                .contentType(ContentType.JSON)
                .body("{\"configuredLevel\": \"INFO\"}")
                .post("/jme-management-test/actuator/loggers/ch.admin.bit.jeap.monitor").then()
                .assertThat().statusCode(204);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "beans",
            "configprops",
            "env",
            "metrics",
            "scheduledtasks",
            "caches",
            "mappings",
            "health",
            "info",
            "prometheus"
    })
    void testEndpointsOtherThanLoggers_notWriteableEvenForActuatorRole(String endpoint) {
        for (HttpMethod httpMethod : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH)) {
            requestWithActuatorRole()
                    .request(httpMethod.name(), "/jme-management-test/actuator/" + endpoint).then()
                    .assertThat().statusCode(403);
        }
    }

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
        String javaVersion = System.getProperty("java.version");
        requestWithPrometheusRole()
                .get("/jme-management-test/actuator/prometheus")
                .then().assertThat()
                .statusCode(200)
                .body(
                        containsString("jeap_java_version{"),
                        containsString("version=\"" + javaVersion),
                        containsString("spring_app{"),
                        containsString("name=\"jme-monitor-test\""));
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
