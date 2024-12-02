package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import org.springframework.http.HttpStatus;

import static ch.admin.bit.jeap.security.it.resource.TestRoles.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class AbstractJeapAccessTokenIT extends AccessTokenITBase {

	protected AbstractJeapAccessTokenIT(int serverPort, String context) {
		super(serverPort, context);
	}

	protected static final String SUBJECT = "69368608-D736-43C8-5F76-55B7BF168299";
	protected static final String EXT_ID = "342809732";
	protected static final String ADMIN_DIR_UID = "U11111111";
	protected static final String NAME = "Max Muster";
	protected static final String FAMILY_NAME = "Muster";
	protected static final String GIVEN_NAME = "Max";
	protected static final String PREFERRED_USER_NAME = "Maximilian";
	protected static final String LOCALE = "de";

	protected static final String PARTNER_ID = "123456789";
	protected static final String OTHER_PARTNER_ID = "987654321";
	protected static final String YET_ANOTHER_PARTNER_ID = "234526543";

	protected void testGetAuth_whenAuthServerJeapTokenWithUserInfoAndRoleAuthRead_thenAccessGrantedAndUserInfoAndRolesCorrect() {
		final String jeapToken = createJeapTokenWithUserInfoForUserRoles(JeapAuthenticationContext.USER, SEMANTIC_AUTH_READ_ROLE, SEMANTIC_OTHER_READ_ROLE);

		given().
				spec(semanticAuthPathSpec).
				auth().oauth2(jeapToken).
		when().
				get().
		then().
				statusCode(HttpStatus.OK.value()).
				body("subject", equalTo(SUBJECT)).
                body("userroles", containsInAnyOrder(SEMANTIC_AUTH_READ_ROLE, SEMANTIC_OTHER_READ_ROLE)).
				body("bproles", anEmptyMap()).
				body("extId", equalTo(EXT_ID)).
				body("adminDirUID", equalTo(ADMIN_DIR_UID)).
				body("locale", equalTo(LOCALE)).
				body("ctx", equalTo(JeapAuthenticationContext.USER.name())).
                body("name", equalTo(NAME)).
                body("givenName", equalTo(GIVEN_NAME)).
                body("familyName", equalTo(FAMILY_NAME)).
                body("preferredUsername", equalTo(PREFERRED_USER_NAME));
	}

	protected void testGetAuth_whenAuthServerJeapTokenWithBpRoleAuthRead_thenAccessGrantedAndRolesCorrect() {
		final String jeapToken = createJeapTokenForBpRoles(SEMANTIC_AUTH_READ_ROLE, SEMANTIC_OTHER_READ_ROLE);

		given().
				spec(semanticAuthForPartnerPathTemplateSpec).
				pathParam(PARTNER_ID_PARAM_NAME, PARTNER_ID).
				auth().oauth2(jeapToken).
		when().
				get().
		then().
				statusCode(HttpStatus.OK.value()).
				body("bproles." + PARTNER_ID, containsInAnyOrder(SEMANTIC_AUTH_READ_ROLE, SEMANTIC_OTHER_READ_ROLE)).
				body("userroles", empty()).
				body("ctx", equalTo(JeapAuthenticationContext.USER.name()));
	}

	private String createJeapTokenWithUserInfoForUserRoles(JeapAuthenticationContext context, String... roles) {
		return jwsBuilderFactory.createValidForFixedLongPeriodBuilder(SUBJECT, context).
				withUserRoles(roles).
				withExtId(EXT_ID).
				withAdminDirUID(ADMIN_DIR_UID).
				withLocale(LOCALE).
                withFamilyName(FAMILY_NAME).
                withGivenName(GIVEN_NAME).
                withName(NAME).
				withPreferredUsername(PREFERRED_USER_NAME).
				build().serialize();
	}

	private String createJeapTokenForBpRoles(String... roles) {
		return jwsBuilderFactory.createValidForFixedLongPeriodBuilder(SUBJECT, JeapAuthenticationContext.USER).
				withBusinessPartnerRoles(AbstractJeapAccessTokenIT.PARTNER_ID, roles).
				withBusinessPartnerRoles(OTHER_PARTNER_ID, SEMANTIC_OTHER_READ_ROLE).
				withBusinessPartnerRoles(YET_ANOTHER_PARTNER_ID, SEMANTIC_YET_ANOTHER_READ_ROLE).
				build().serialize();
	}

}
