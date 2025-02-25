package ch.admin.bit.jeap.security.it.resource.webflux;

import ch.admin.bit.jeap.security.it.resource.AbstractCurrentUserControllerAuthorizationIT;
import ch.admin.bit.jeap.security.user.JeapCurrentUser;
import ch.admin.bit.jeap.security.user.JeapCurrentUserCustomizer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import(CurrentUserControllerSemanticAuthorizationCustomCustomizerWebFluxIT.IntegrationTestConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=8037", "jeap.security.oauth2.current-user-endpoint.enabled=true"})
class CurrentUserControllerSemanticAuthorizationCustomCustomizerWebFluxIT extends AbstractCurrentUserControllerAuthorizationIT {

    protected CurrentUserControllerSemanticAuthorizationCustomCustomizerWebFluxIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
        super(serverPort, context);
    }

    @Test
    @SneakyThrows
    void getCurrentUser_full() {
        testGetCurrentUserFull(
                """
                        {
                            "subject": "subject",
                            "foo": "foo",
                            "bar": "bar"
                        }
                        
                        """, true
        );
    }

    @Data
    @AllArgsConstructor
    static class JeapCustomCurrentUserTestDto implements JeapCurrentUser {
        String subject;
        String foo;
        String bar;
    }

    @TestConfiguration
    static class IntegrationTestConfiguration {
        @Bean
        public JeapCurrentUserCustomizer<JeapCustomCurrentUserTestDto> jeapCustomCurrentUserMapper() {
            return jeapCurrentUser -> new JeapCustomCurrentUserTestDto(jeapCurrentUser.getSubject(), "foo", "bar");
        }
    }
}
