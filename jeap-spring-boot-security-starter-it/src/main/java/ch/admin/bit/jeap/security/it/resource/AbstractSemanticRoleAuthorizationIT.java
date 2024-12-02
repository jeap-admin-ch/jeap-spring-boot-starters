package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import org.springframework.http.HttpStatus;

import static ch.admin.bit.jeap.security.it.resource.TestRoles.*;

public class AbstractSemanticRoleAuthorizationIT extends AccessTokenITBase {

	protected AbstractSemanticRoleAuthorizationIT(int serverPort, String context) {
		super(serverPort, context);
	}

	protected static final String SUBJECT = "69368608-D736-43C8-5F76-55B7BF168299";
	protected static final String PARTNER_ID = "123456789";
	protected static final String OTHER_PARTNER_ID = "987654321";

	protected void testGetAuth_whenWithUserRoleAuthRead_thenAccessGranted() {
		final String jeapToken = createJeapTokenWithUserRoles(SEMANTIC_AUTH_READ_ROLE);
		assertHttpStatusWithTokenOnGet(semanticAuthPathSpec, jeapToken, HttpStatus.OK);
		assertHttpStatusWithTokenOnGet(semanticProgrammaticAuthPathSpec, jeapToken, HttpStatus.OK);
	}

	protected void testGetAuth_whenOnlyWithUserRoleDifferentThanAuthRead_thenAccessDenied() {
		final String jeapToken = createJeapTokenWithUserRoles(SEMANTIC_OTHER_READ_ROLE);
		assertHttpStatusWithTokenOnGet(semanticAuthPathSpec, jeapToken, HttpStatus.FORBIDDEN);
		assertHttpStatusWithTokenOnGet(semanticProgrammaticAuthPathSpec, jeapToken, HttpStatus.FORBIDDEN);
	}

	protected void testGetAuthForPartner_whenWithBpRoleAuthReadForQueriedPartner_thenAccessGranted() {
		final String jeapToken = createJeapTokenForBpRoles(SEMANTIC_AUTH_READ_ROLE);
		assertHttpStatusWithTokenOnGetForPartner(semanticAuthForPartnerPathTemplateSpec, jeapToken, PARTNER_ID, HttpStatus.OK);
		assertHttpStatusWithTokenOnGetForPartner(semanticProgrammaticAuthForPartnerPathTemplateSpec, jeapToken, PARTNER_ID, HttpStatus.OK);
	}

	protected void testGetAuthForPartner_whenWithoutBpRoleAuthReadForQueriedPartner_thenAccessDenied() {
		final String jeapToken = createJeapTokenForBpRoles(SEMANTIC_AUTH_READ_ROLE);
		assertHttpStatusWithTokenOnGetForPartner(semanticAuthForPartnerPathTemplateSpec, jeapToken, OTHER_PARTNER_ID, HttpStatus.FORBIDDEN);
		assertHttpStatusWithTokenOnGetForPartner(semanticProgrammaticAuthForPartnerPathTemplateSpec, jeapToken, OTHER_PARTNER_ID, HttpStatus.FORBIDDEN);
	}

	private String createJeapTokenWithUserRoles(String... roles) {
		return jwsBuilderFactory.createValidForFixedLongPeriodBuilder(SUBJECT, JeapAuthenticationContext.SYS).
				withUserRoles(roles).
				build().serialize();
	}

	private String createJeapTokenForBpRoles(String... roles) {
		return jwsBuilderFactory.createValidForFixedLongPeriodBuilder(SUBJECT, JeapAuthenticationContext.USER).
				withBusinessPartnerRoles(PARTNER_ID, roles).
				build().serialize();
	}

}
