package ch.admin.bit.jeap.monitor;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;

@SpringBootTest(
        classes = TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.endpoint.beans.enabled=true"
)
@AutoConfigureObservability
class ActuatorSecurityAdditionalEndpointConfigWebmvcIT {

    @LocalManagementPort
    int localManagementPort;

    @Test
    void testBeansEndpoint_notAccessibleForAnyRoleWhenAdminNotEnabled() {
        requestWithoutRoles()
                .get("/jme-management-test/actuator/beans")
                .then().assertThat()
                .statusCode(401);
        requestWithActuatorRole()
                .get("/jme-management-test/actuator/beans")
                .then().assertThat()
                .statusCode(403);
    }

    private RequestSpecification requestWithoutRoles() {
        return RestAssured.given()
                .port(localManagementPort);
    }

    private RequestSpecification requestWithActuatorRole() {
        return requestWithoutRoles()
                .auth().basic("actuator", "test");
    }
}
