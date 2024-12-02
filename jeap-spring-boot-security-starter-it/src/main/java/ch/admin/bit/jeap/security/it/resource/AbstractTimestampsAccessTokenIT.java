package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.ZonedDateTime;

import static ch.admin.bit.jeap.security.it.resource.TestRoles.SEMANTIC_AUTH_READ_ROLE;
import static ch.admin.bit.jeap.security.test.jws.JwsBuilder.*;


public class AbstractTimestampsAccessTokenIT extends AccessTokenITBase {

	private static final String SUBJECT = "69368608-D736-43C8-5F76-55B7BF168299";
	private static final Duration LEEWAY = Duration.ofSeconds(30); // leeway because of possible clock skew

	protected AbstractTimestampsAccessTokenIT(int serverPort, String context) {
		super(serverPort, context);
	}

	protected void testGetAuth_whenJeapTokenValidInContext_thenAccessGranted(JeapAuthenticationContext context) {
		final ZonedDateTime now = ZonedDateTime.now();
		final ZonedDateTime issuedAt = now;
		final ZonedDateTime notBefore = now;
		final ZonedDateTime expiry = now.plusMinutes(3);
		final String jeapToken = createJeapToken(expiry, notBefore, issuedAt, context, SEMANTIC_AUTH_READ_ROLE);

		assertHttpStatusWithTokenOnGetToAuthResource(jeapToken, HttpStatus.OK);
	}

	protected void testGetAuth_whenJeapTokenExpiredInContext_thenAccessDenied(JeapAuthenticationContext context) {
		final ZonedDateTime now = ZonedDateTime.now();
		final ZonedDateTime issuedAt = now.minusMinutes(5);
		final ZonedDateTime notBefore = issuedAt;
		final ZonedDateTime expiry = now.minus(LEEWAY).minusSeconds(1);
		final String jeapToken = createJeapToken(expiry, notBefore, issuedAt, context, SEMANTIC_AUTH_READ_ROLE);

		assertHttpStatusWithTokenOnGetToAuthResource(jeapToken,HttpStatus.UNAUTHORIZED);
	}

	protected void testGetAuth_whenJeapTokenExpiredWithinLeewayInContext_thenAccessGranted(JeapAuthenticationContext context) {
		final ZonedDateTime now = ZonedDateTime.now();
		final ZonedDateTime issuedAt = now.minusMinutes(5);
		final ZonedDateTime notBefore = issuedAt;
		final ZonedDateTime expiry = now.minus(LEEWAY).plusSeconds(3);
		final String jeapToken = createJeapToken(expiry, notBefore, issuedAt, context, SEMANTIC_AUTH_READ_ROLE);

		assertHttpStatusWithTokenOnGetToAuthResource(jeapToken, HttpStatus.OK);
	}

	protected void testGetAuth_whenJeapTokenNotBeforeIsAfterNowInContext_thenAccessDenied(JeapAuthenticationContext context) {
		final ZonedDateTime now = ZonedDateTime.now();
		final ZonedDateTime issuedAt = now;
		final ZonedDateTime notBefore = now.plus(LEEWAY).plusSeconds(3);
		final ZonedDateTime expiry = now.plusMinutes(5);
		final String jeapToken = createJeapToken(expiry, notBefore, issuedAt, context, SEMANTIC_AUTH_READ_ROLE);

		assertHttpStatusWithTokenOnGetToAuthResource(jeapToken,HttpStatus.UNAUTHORIZED);
	}

	protected void testGetAuth_whenJeapTokenNotBeforeIsAfterNowButWithinLeewayInContext_thenAccessDenied(JeapAuthenticationContext context) {
		final ZonedDateTime now = ZonedDateTime.now();
		final ZonedDateTime issuedAt = now;
		final ZonedDateTime notBefore = now.plus(LEEWAY);
		final ZonedDateTime expiry = now.plusMinutes(5);
		final String jeapToken = createJeapToken(expiry, notBefore, issuedAt, context, SEMANTIC_AUTH_READ_ROLE);

		assertHttpStatusWithTokenOnGetToAuthResource(jeapToken, HttpStatus.OK);
	}

	private String createJeapToken(ZonedDateTime expiry, ZonedDateTime notBefore, ZonedDateTime issuedAt, JeapAuthenticationContext context, String... roles) {
		return jwsBuilderFactory.createBuilder(DEFAULT_JTI, getIssuerForContext(context), expiry, notBefore, issuedAt, SUBJECT, context).
				withUserRoles(roles).
				build().serialize();
	}

}
