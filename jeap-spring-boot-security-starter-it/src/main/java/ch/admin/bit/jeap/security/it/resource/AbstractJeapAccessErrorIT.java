package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import org.springframework.http.HttpStatus;

import static ch.admin.bit.jeap.security.it.resource.TestRoles.SEMANTIC_AUTH_READ_ROLE;
import static ch.admin.bit.jeap.security.it.resource.TestRoles.SEMANTIC_OTHER_READ_ROLE;
import static io.restassured.RestAssured.given;

public class AbstractJeapAccessErrorIT extends AccessTokenITBase {

	protected AbstractJeapAccessErrorIT(int serverPort, String context) {
		super(serverPort, context);
	}

	protected void testGetAuth_whenAuthorizedToAccessErrorEndpoint_ThenHttp500() {
		final String jeapToken = createJeapTokenWithSemanticAuthReadRole();

		given().
				spec(errorAuthPathSpec).
				auth().oauth2(jeapToken).
		when().
				get().
		then().
				statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
	}

	protected void testGetAuth_whenNoBearerTokenSet_ThenUnauthorized() {
		given().
				spec(errorAuthPathSpec).
		when().
				get().
		then().
				statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	protected void testGetAuth_whenBearerTokenIsMissingRoleToAccessErrorEndpoint_ThenForbidden() {
		final String jeapToken = createJeapTokenWithSemanticOtherReadRole();

		given().
				spec(errorAuthPathSpec).
				auth().oauth2(jeapToken).
		when().
				get().
		then().
				statusCode(HttpStatus.FORBIDDEN.value());
	}

	private String createJeapTokenWithSemanticAuthReadRole() {
		return jwsBuilderFactory.createValidForFixedLongPeriodBuilder("subject", JeapAuthenticationContext.USER)
				.withUserRoles(SEMANTIC_AUTH_READ_ROLE)
				.build().serialize();
	}

	private String createJeapTokenWithSemanticOtherReadRole() {
		return jwsBuilderFactory.createValidForFixedLongPeriodBuilder("subject", JeapAuthenticationContext.USER)
				.withUserRoles(SEMANTIC_OTHER_READ_ROLE)
				.build().serialize();
	}

}