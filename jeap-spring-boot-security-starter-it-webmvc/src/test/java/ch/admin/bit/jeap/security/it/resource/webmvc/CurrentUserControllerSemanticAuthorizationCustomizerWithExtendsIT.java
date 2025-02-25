package ch.admin.bit.jeap.security.it.resource.webmvc;

import ch.admin.bit.jeap.security.it.resource.AbstractCurrentUserControllerAuthorizationIT;
import ch.admin.bit.jeap.security.user.JeapCurrentUser;
import ch.admin.bit.jeap.security.user.JeapCurrentUserCustomizer;
import ch.admin.bit.jeap.security.user.JeapCurrentUserDto;
import lombok.Getter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import(CurrentUserControllerSemanticAuthorizationCustomizerWithExtendsIT.IntegrationTestConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=8022", "jeap.security.oauth2.current-user-endpoint.enabled=true"})
class CurrentUserControllerSemanticAuthorizationCustomizerWithExtendsIT extends AbstractCurrentUserControllerAuthorizationIT {

    protected CurrentUserControllerSemanticAuthorizationCustomizerWithExtendsIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
        super(serverPort, context);
    }

    @Test
    @SneakyThrows
    void getCurrentUser_full() {
       testGetCurrentUserFull(
               """
                                {
                                    "foo": "foo",
                                    "bar": "bar",
                                    "subject": "subject",
                                    "name": "name",
                                    "preferredUsername": "preferredUsername",
                                    "familyName": "familyName",
                                    "givenName": "givenName",
                                    "locale": "locale",
                                    "authenticationContextClassReference": "acr",
                                    "authenticationMethodsReferences": [
                                        "amr1",
                                        "amr2"
                                    ],
                                    "userExtId": "extId",
                                    "userRoles": [
                                        "test2"
                                    ],
                                    "businessPartnerRoles": {
                                        "12345": [
                                            "roleA"
                                        ]
                                    }
                                }
                       """, true
       );
    }

    @Getter
    static class JeapCustomCurrentUserTestDto extends JeapCurrentUserDto {
        String foo;
        String bar;

        public JeapCustomCurrentUserTestDto(JeapCurrentUser jeapCurrentUser, String foo, String bar) {
            super(jeapCurrentUser);
            this.foo = foo;
            this.bar = bar;
            setAdminDirUid(null);
            setPamsLoginLevel(null);
        }
    }

    @TestConfiguration
    static class IntegrationTestConfiguration {
        @Bean
        public JeapCurrentUserCustomizer<JeapCustomCurrentUserTestDto> jeapCurrentUserTestCustomizer() {
            return jeapCurrentUser -> new JeapCustomCurrentUserTestDto(jeapCurrentUser, "foo", "bar");
        }
    }
}
