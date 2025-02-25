package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.user.JeapCurrentUserDto;
import com.nimbusds.jwt.SignedJWT;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("java:S5960")
public class AbstractCurrentUserControllerAuthorizationIT extends AccessTokenITBase {

    private static final String SUBJECT = "subject";
    private final RequestSpecification requestSpecification;

    protected AbstractCurrentUserControllerAuthorizationIT(int serverPort, String context) {
        super(serverPort, context);
        this.requestSpecification = new RequestSpecBuilder().setBasePath(baseUrl + "/current-user").setPort(serverPort).build();
    }

    protected AbstractCurrentUserControllerAuthorizationIT(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context, String path) {
        super(serverPort, context);
        this.requestSpecification = new RequestSpecBuilder().setBasePath(baseUrl + path).setPort(serverPort).build();
    }

    @SneakyThrows
    protected void testGetCurrentUserFull() {

        testGetCurrentUserFull(
                """
            {
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
                "adminDirUid": "adminDirUID",
                "userExtId": "extId",
                "userRoles": [
                    "test2"
                ],
                "businessPartnerRoles": {
                    "12345": [
                        "roleA"
                    ]
                },
                "pamsLoginLevel": "loginLevel"
            }
        """, false
        );

    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    protected void testGetCurrentUserFull(String expectedResponse, boolean isCustom) {
        final SignedJWT jeapToken = jwsBuilderFactory
                .createValidForFixedLongPeriodBuilder(SUBJECT, JeapAuthenticationContext.USER).
                withBusinessPartnerRoles("12345", "roleA").
                withFamilyName("familyName").
                withGivenName("givenName").
                withExtId("extId").
                withPreferredUsername("preferredUsername").
                withLocale("locale").
                withClaim("login_level", "loginLevel").
                withClaim("acr", "acr").
                withClaim("amr", List.of("amr1", "amr2")).
                withAdminDirUID("adminDirUID").
                withName("name").
                withClaim(SUBJECT, SUBJECT).
                withUserRoles("test2").
                build();

        String stringResponse = given().
                spec(requestSpecification).
                auth().oauth2(jeapToken.serialize()).
                when().
                get().
                then().
                statusCode(200).
                extract().response().body().prettyPrint();

        assertThat(stringResponse).isEqualToIgnoringWhitespace(expectedResponse);

        if (!isCustom) {
            JeapCurrentUserDto currentUserDto = given().
                    spec(requestSpecification).
                    auth().oauth2(jeapToken.serialize()).
                    when().
                    get().
                    then().
                    statusCode(200).
                    extract().as(JeapCurrentUserDto.class);

            assertThat(currentUserDto.getSubject()).isEqualTo(jeapToken.getJWTClaimsSet().getSubject());
            assertThat(currentUserDto.getName()).isEqualTo(jeapToken.getJWTClaimsSet().getClaim("name"));
            assertThat(currentUserDto.getPreferredUsername()).isEqualTo(jeapToken.getJWTClaimsSet().getClaim("preferred_username"));
            assertThat(currentUserDto.getFamilyName()).isEqualTo(jeapToken.getJWTClaimsSet().getClaim("family_name"));
            assertThat(currentUserDto.getGivenName()).isEqualTo(jeapToken.getJWTClaimsSet().getClaim("given_name"));
            assertThat(currentUserDto.getLocale()).isEqualTo(jeapToken.getJWTClaimsSet().getClaim("locale"));
            assertThat(currentUserDto.getAuthenticationContextClassReference()).isEqualTo(jeapToken.getJWTClaimsSet().getClaim("acr"));
            assertThat(currentUserDto.getAuthenticationMethodsReferences()).isEqualTo(jeapToken.getJWTClaimsSet().getClaim("amr"));
            assertThat(currentUserDto.getAdminDirUid()).isEqualTo(jeapToken.getJWTClaimsSet().getClaim("admin_dir_uid"));
            assertThat(currentUserDto.getUserExtId()).isEqualTo(jeapToken.getJWTClaimsSet().getClaim("ext_id"));
            assertThat(currentUserDto.getUserRoles()).containsExactlyElementsOf((List<String>) jeapToken.getJWTClaimsSet().getClaim("userroles"));
            assertThat(currentUserDto.getBusinessPartnerRoles()).isEqualTo(jeapToken.getJWTClaimsSet().getClaim("bproles"));
            assertThat(currentUserDto.getPamsLoginLevel()).isEqualTo(jeapToken.getJWTClaimsSet().getClaim("login_level"));
        }
    }

    @SneakyThrows
    protected void testGetCurrentUserBpRoles() {
        final SignedJWT jeapToken = jwsBuilderFactory
                .createValidForFixedLongPeriodBuilder(SUBJECT, JeapAuthenticationContext.USER).
                withBusinessPartnerRoles("12345", "role1", "role2").
                withBusinessPartnerRoles("45678", "role3").
                build();

        JeapCurrentUserDto jeapCurrentUserDto = given().
                spec(requestSpecification).
                auth().oauth2(jeapToken.serialize()).
                when().
                get().
                then().
                statusCode(200).
                extract().as(JeapCurrentUserDto.class);

        assertThat(jeapCurrentUserDto.getSubject()).isEqualTo(jeapToken.getJWTClaimsSet().getSubject());
        assertThat(jeapCurrentUserDto.getBusinessPartnerRoles()).isEqualTo(jeapToken.getJWTClaimsSet().getClaim("bproles"));
    }

    @SneakyThrows
    protected void testGetCurrentUserUserRoles() {
        final SignedJWT jeapToken = jwsBuilderFactory
                .createValidForFixedLongPeriodBuilder(SUBJECT, JeapAuthenticationContext.USER).
                withUserRoles("role100", "role200").
                build();

        JeapCurrentUserDto jeapCurrentUserDto = given().
                spec(requestSpecification).
                auth().oauth2(jeapToken.serialize()).
                when().
                get().
                then().
                statusCode(200).
                extract().as(JeapCurrentUserDto.class);

        assertThat(jeapCurrentUserDto.getSubject()).isEqualTo(jeapToken.getJWTClaimsSet().getSubject());
        assertThat(jeapCurrentUserDto.getUserRoles()).containsExactlyInAnyOrder("role100", "role200");
    }
}
