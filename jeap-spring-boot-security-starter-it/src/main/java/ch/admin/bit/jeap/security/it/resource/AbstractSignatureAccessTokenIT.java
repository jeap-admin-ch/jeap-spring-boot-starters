package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.test.jws.JwsBuilder;
import org.springframework.http.HttpStatus;

import static ch.admin.bit.jeap.security.it.resource.TestRoles.SEMANTIC_AUTH_READ_ROLE;

public class AbstractSignatureAccessTokenIT extends AccessTokenITBase {

	private static final String SUBJECT = "69368608-D736-43C8-5F76-55B7BF168299";

	protected AbstractSignatureAccessTokenIT(int serverPort, String context) {
		super(serverPort, context);
	}

	protected void testGetAuth_whenJeapTokenSignedWithCorrectKeyInContext_thenAccessGranted(JeapAuthenticationContext context) {
		final JwsBuilder jwsBuilder = jwsBuilderFactory.createValidForFixedLongPeriodBuilder(SUBJECT, context);
		final String jeapToken = createJeapToken(jwsBuilder);

		assertHttpStatusWithTokenOnGetToAuthResource(jeapToken, HttpStatus.OK);
	}

	protected void testGetAuth_whenJeapTokenSignedWithWrongKeyInContext_thenAccessDenied(JeapAuthenticationContext context) {
		// If no RSA key is set, JwsBuilder will create a new one on its own
		final JwsBuilder jwsBuilder = JwsBuilder.createValidForFixedLongPeriod(SUBJECT, context);
		final String jeapToken = createJeapToken(jwsBuilder);

		assertHttpStatusWithTokenOnGetToAuthResource(jeapToken, HttpStatus.UNAUTHORIZED);
	}

	private String createJeapToken(JwsBuilder jwsBuilder) {
		return jwsBuilder.withUserRoles(SEMANTIC_AUTH_READ_ROLE).build().serialize();
	}

}
