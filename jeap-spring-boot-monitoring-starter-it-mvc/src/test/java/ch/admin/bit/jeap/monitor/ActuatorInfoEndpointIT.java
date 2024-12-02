package ch.admin.bit.jeap.monitor;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;

import static org.hamcrest.Matchers.containsString;

@SpringBootTest(
        classes = TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"info.config.junitTestKey=junitTestValue"})
class ActuatorInfoEndpointIT {

    @LocalManagementPort
    int localManagementPort;

    @Test
    void testInfoEndpoint_showsApplicationProperties() {
        requestWithoutRoles()
                .get("/jme-management-test/actuator/info")
                .then().assertThat()
                .statusCode(200)
                .body(containsString("\"config\":{\"junitTestKey\":\"junitTestValue\"}"));
    }

    private RequestSpecification requestWithoutRoles() {
        return RestAssured.given()
                .port(localManagementPort);
    }

}
