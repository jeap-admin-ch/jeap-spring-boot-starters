package ch.admin.bit.jeap.security.it.resource.webflux;

import ch.admin.bit.jeap.security.it.resource.AbstractCurrentUserControllerAuthorizationIT;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
        "server.port=8019",
        "jeap.security.oauth2.current-user-endpoint.enabled=true",
        "jeap.security.oauth2.current-user-endpoint.path=/api/custom-path"
})
class CurrentUserControllerSimpleAuthorizationCustomConfigWebFluxIT extends AbstractCurrentUserControllerAuthorizationIT {

    protected CurrentUserControllerSimpleAuthorizationCustomConfigWebFluxIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
        super(serverPort, context, "/custom-path");

    }

    @Test
    @SneakyThrows
    void getCurrentUser_full() {
        testGetCurrentUserFull();
    }

    @Test
    @SneakyThrows
    void getCurrentUser_bpRoles() {
        testGetCurrentUserBpRoles();
    }

    @Test
    @SneakyThrows
    void getCurrentUser_userRoles() {
        testGetCurrentUserUserRoles();
    }
}
