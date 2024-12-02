package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.test.jws.JwsBuilder;
import org.springframework.http.HttpStatus;

import static ch.admin.bit.jeap.security.it.resource.TestRoles.SEMANTIC_AUTH_READ_ROLE;

public class AbstractAudienceAccessTokenIT extends AccessTokenITBase {

	protected AbstractAudienceAccessTokenIT(int serverPort, String context) {
		super(serverPort, context);
	}

	protected void testGetAuth_whenJeapTokenAudienceEmptyInContext_thenAccessGranted(JeapAuthenticationContext context) {
		final String jeapToken = createJeapTokenForAudienceWithUserroles(null, context, SEMANTIC_AUTH_READ_ROLE);

		assertHttpStatusWithTokenOnGetToAuthResource(jeapToken, HttpStatus.OK);
	}

	protected void testGetAuth_whenJeapTokenAudienceContainsAppNameInContext_thenAccessGranted(JeapAuthenticationContext context) {
		final String jeapToken = createJeapTokenForAudienceWithUserroles(applicationName, context, SEMANTIC_AUTH_READ_ROLE);

		assertHttpStatusWithTokenOnGetToAuthResource(jeapToken, HttpStatus.OK);
	}

	protected void testGetAuth_whenJeapTokenAudienceDoesNotContainsAppNameInContext_thenAccessDenied(JeapAuthenticationContext context) {
		final String jeapToken = createJeapTokenForAudienceWithUserroles("other", context, SEMANTIC_AUTH_READ_ROLE);

		assertHttpStatusWithTokenOnGetToAuthResource(jeapToken, HttpStatus.UNAUTHORIZED);
	}

	protected void testGetAuth_whenJeapTokenAudienceDoesNotContainsAppNameInContext_thenAccessGranted(JeapAuthenticationContext context) {
		final String jeapToken = createJeapTokenForAudienceWithUserroles("other", context, SEMANTIC_AUTH_READ_ROLE);

		assertHttpStatusWithTokenOnGetToAuthResource(jeapToken, HttpStatus.OK);
	}

	private String createJeapTokenForAudienceWithUserroles(String audience, JeapAuthenticationContext context, String... userroles) {
		JwsBuilder jwsBuilder = jwsBuilderFactory.createValidForFixedLongPeriodBuilder("69368608-D736-43C8-5F76-55B7BF168299", context);
		if (audience != null) {
			jwsBuilder.withAudiences(audience);
		}
		jwsBuilder.withUserRoles(userroles);
		return jwsBuilder.build().serialize();
	}

}
