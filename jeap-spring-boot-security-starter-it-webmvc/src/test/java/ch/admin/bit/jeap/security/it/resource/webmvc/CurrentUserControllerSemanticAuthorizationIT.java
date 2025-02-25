package ch.admin.bit.jeap.security.it.resource.webmvc;

import ch.admin.bit.jeap.security.it.resource.AbstractCurrentUserControllerAuthorizationIT;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=8017", "jeap.security.oauth2.current-user-endpoint.enabled=true"})
class CurrentUserControllerSemanticAuthorizationIT extends AbstractCurrentUserControllerAuthorizationIT {

    protected CurrentUserControllerSemanticAuthorizationIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
        super(serverPort, context);
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
