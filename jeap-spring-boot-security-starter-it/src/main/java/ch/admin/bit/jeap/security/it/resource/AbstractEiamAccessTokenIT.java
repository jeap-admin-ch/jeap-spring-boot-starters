package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

import static ch.admin.bit.jeap.security.it.resource.TestRoles.SEMANTIC_AUTH_READ_ROLE;
import static ch.admin.bit.jeap.security.it.resource.TestRoles.SEMANTIC_OTHER_READ_ROLE;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

public class AbstractEiamAccessTokenIT extends AccessTokenITBase {

	protected static final String SUBJECT = "69368608-D736-43C8-5F76-55B7BF168299";
	protected static final String EXT_ID = "342809732";
	protected static final String LOCALE = "DE";
	protected static final String ADMIN_DIR_UID = "U11111111";

    protected AbstractEiamAccessTokenIT(int serverPort, String context) {
		super(serverPort, context);
	}

	protected void testGetAuth_whenAuthServerEiamTokenWithRoleAuthRead_thenConvertedCorrectlyToJeapTokenAndAccessGranted() {
        final String eiamToken = createEiamTokenForRoles(
				JeapAuthenticationContext.SYS, // will be overwritten with USER by claim set converter
				SEMANTIC_AUTH_READ_ROLE, SEMANTIC_OTHER_READ_ROLE);

        given().
                spec(semanticAuthPathSpec).
                auth().oauth2(eiamToken).
        when().
                get().
        then().
                statusCode(HttpStatus.OK.value()).
				body("userroles", containsInAnyOrder(SEMANTIC_AUTH_READ_ROLE, SEMANTIC_OTHER_READ_ROLE)).
				body("extId", equalTo(EXT_ID)).
				body("locale", equalTo(LOCALE)).
				body("ctx", equalTo(JeapAuthenticationContext.USER.name()));
    }

	protected void testGetAuth_whenAuthServerJeapTokenWithRoleAuthRead_thenCtxSetByEiamClaimSetConverterAndAccessGranted() {
		final String jeapToken = createJeapTokenForRoles(
				JeapAuthenticationContext.SYS, // will be overwritten with USER by claim set converter
				SEMANTIC_AUTH_READ_ROLE, SEMANTIC_OTHER_READ_ROLE);

		given().
				spec(semanticAuthPathSpec).
				auth().oauth2(jeapToken).
		when().
				get().
		then().
				statusCode(HttpStatus.OK.value()).
				body("userroles", containsInAnyOrder(SEMANTIC_AUTH_READ_ROLE, SEMANTIC_OTHER_READ_ROLE)).
				body("extId", equalTo(EXT_ID)).
				body("adminDirUID", equalTo(ADMIN_DIR_UID)).
				body("locale", equalTo(LOCALE)).
				body("ctx", equalTo(JeapAuthenticationContext.USER.name()));
	}

	protected void testGetAuth_whenAuthServerEiamTokenWithoutRoleAuthRead_thenAccessDenied() {
		final String eiamTokenWithoutRoleAuthRead = createEiamTokenForRoles(JeapAuthenticationContext.SYS, SEMANTIC_OTHER_READ_ROLE);

		given().
				spec(semanticAuthPathSpec).
				auth().oauth2(eiamTokenWithoutRoleAuthRead).
		when().
				get().
		then().
				statusCode(HttpStatus.FORBIDDEN.value());
	}

	protected void testGetAuth_whenB2BJeapTokenWithRoleAuthRead_thenEiamClaimSetConverterNotActiveAndAccessGranted() {
		final String jeapToken = createJeapTokenForRoles(
				JeapAuthenticationContext.B2B, // will not be overwritten as there is no claim set converter configured for B2B
				SEMANTIC_AUTH_READ_ROLE, SEMANTIC_OTHER_READ_ROLE);

		given().
				spec(semanticAuthPathSpec).
				auth().oauth2(jeapToken).
		when().
				get().
		then().
				statusCode(HttpStatus.OK.value()).
				body("userroles", containsInAnyOrder(SEMANTIC_AUTH_READ_ROLE, SEMANTIC_OTHER_READ_ROLE)).
				body("extId", equalTo(EXT_ID)).
				body("locale", equalTo(LOCALE)).
				body("adminDirUID", equalTo(ADMIN_DIR_UID)).
				body("ctx", equalTo(JeapAuthenticationContext.B2B.name()));
	}

	protected void testGetAuth_whenB2BEiamTokenWithRoleAuthRead_thenAccessDeniedAsNoJeapTokenClaimsArePresent() {
		final String eiamTokenWithoutRoleAuthRead = createEiamTokenForRoles(JeapAuthenticationContext.B2B, SEMANTIC_AUTH_READ_ROLE, SEMANTIC_OTHER_READ_ROLE);

		given().
				spec(semanticAuthPathSpec).
				auth().oauth2(eiamTokenWithoutRoleAuthRead).
		when().
				get().
		then().
				statusCode(HttpStatus.FORBIDDEN.value());
	}

	private String createEiamTokenForRoles(JeapAuthenticationContext context, String... roles) {
		return jwsBuilderFactory.createValidForFixedLongPeriodBuilder(SUBJECT, context).
				withClaim("role", Arrays.asList(roles)).
				withClaim("userExtId", EXT_ID).
				withClaim("language", LOCALE.toLowerCase()).
				build().serialize();
	}

	private String createJeapTokenForRoles(JeapAuthenticationContext context, String... roles) {
		return jwsBuilderFactory.createValidForFixedLongPeriodBuilder(SUBJECT, context).
				withUserRoles(roles).
				withExtId(EXT_ID).
				withLocale(LOCALE).
				withAdminDirUID("U11111111").
				build().serialize();
	}

}
