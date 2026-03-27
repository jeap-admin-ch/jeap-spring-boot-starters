package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import org.springframework.http.HttpStatus;

import static ch.admin.bit.jeap.security.it.resource.TestRoles.*;

@SuppressWarnings("java:S100")
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

	protected void testGetAuth_whenWithUserRoleAuthReadAlternativeSyntax_thenAccessGranted() {
		final String jeapToken = createJeapTokenWithUserRoles(SEMANTIC_AUTH_READ_ROLE_ALTERNATIVE_SYNTAX);
		assertHttpStatusWithTokenOnGet(semanticAuthPathSpec, jeapToken, HttpStatus.OK);
		assertHttpStatusWithTokenOnGet(semanticProgrammaticAuthPathSpec, jeapToken, HttpStatus.OK);
	}

	protected void testGetAuth_whenOnlyWithUserRoleDifferentThanAuthRead_thenAccessDenied() {
		final String jeapToken = createJeapTokenWithUserRoles(SEMANTIC_OTHER_READ_ROLE);
		assertHttpStatusWithTokenOnGet(semanticAuthPathSpec, jeapToken, HttpStatus.FORBIDDEN);
		assertHttpStatusWithTokenOnGet(semanticProgrammaticAuthPathSpec, jeapToken, HttpStatus.FORBIDDEN);
	}

	protected void testGetAuth_whenOnlyWithUserRoleDifferentThanAuthReadAlternativeSyntax_thenAccessDenied() {
		final String jeapToken = createJeapTokenWithUserRoles(SEMANTIC_OTHER_READ_ROLE_ALTERNATIVE_SYNTAX);
		assertHttpStatusWithTokenOnGet(semanticAuthPathSpec, jeapToken, HttpStatus.FORBIDDEN);
		assertHttpStatusWithTokenOnGet(semanticProgrammaticAuthPathSpec, jeapToken, HttpStatus.FORBIDDEN);
	}

	protected void testGetAuthForPartner_whenWithBpRoleAuthReadForQueriedPartner_thenAccessGranted() {
		final String jeapToken = createJeapTokenForBpRoles(SEMANTIC_AUTH_READ_ROLE);
		assertHttpStatusWithTokenOnGetForPartner(semanticAuthForPartnerPathTemplateSpec, jeapToken, PARTNER_ID, HttpStatus.OK);
		assertHttpStatusWithTokenOnGetForPartner(semanticProgrammaticAuthForPartnerPathTemplateSpec, jeapToken, PARTNER_ID, HttpStatus.OK);
	}

	protected void testGetAuthForPartner_whenWithBpRoleAuthReadForQueriedPartnerAlternativeSyntax_thenAccessGranted() {
		final String jeapToken = createJeapTokenForBpRoles(SEMANTIC_AUTH_READ_ROLE_ALTERNATIVE_SYNTAX);
		assertHttpStatusWithTokenOnGetForPartner(semanticAuthForPartnerPathTemplateSpec, jeapToken, PARTNER_ID, HttpStatus.OK);
		assertHttpStatusWithTokenOnGetForPartner(semanticProgrammaticAuthForPartnerPathTemplateSpec, jeapToken, PARTNER_ID, HttpStatus.OK);
	}

	protected void testGetAuthForPartner_whenWithoutBpRoleAuthReadForQueriedPartner_thenAccessDenied() {
		final String jeapToken = createJeapTokenForBpRoles(SEMANTIC_AUTH_READ_ROLE);
		assertHttpStatusWithTokenOnGetForPartner(semanticAuthForPartnerPathTemplateSpec, jeapToken, OTHER_PARTNER_ID, HttpStatus.FORBIDDEN);
		assertHttpStatusWithTokenOnGetForPartner(semanticProgrammaticAuthForPartnerPathTemplateSpec, jeapToken, OTHER_PARTNER_ID, HttpStatus.FORBIDDEN);
	}

	protected void testGetAuthForPartner_whenWithoutBpRoleAuthReadForQueriedPartnerAlternativeSyntax_thenAccessDenied() {
		final String jeapToken = createJeapTokenForBpRoles(SEMANTIC_AUTH_READ_ROLE_ALTERNATIVE_SYNTAX);
		assertHttpStatusWithTokenOnGetForPartner(semanticAuthForPartnerPathTemplateSpec, jeapToken, OTHER_PARTNER_ID, HttpStatus.FORBIDDEN);
		assertHttpStatusWithTokenOnGetForPartner(semanticProgrammaticAuthForPartnerPathTemplateSpec, jeapToken, OTHER_PARTNER_ID, HttpStatus.FORBIDDEN);
	}

	protected void testGetAuth_whenWithUserRoleAuthRead_thenAccessGrantedViaHasOperation() {
		final String jeapToken = createJeapTokenWithUserRoles(SEMANTIC_AUTH_READ_ROLE);
		assertHttpStatusWithTokenOnGet(semanticOperationAuthPathSpec, jeapToken, HttpStatus.OK);
	}

	protected void testGetAuth_whenWithUserRoleWithDifferentOperation_thenAccessDeniedViaHasOperation() {
		final String jeapToken = createJeapTokenWithUserRoles(SEMANTIC_AUTH_WRITE_ROLE);
		assertHttpStatusWithTokenOnGet(semanticOperationAuthPathSpec, jeapToken, HttpStatus.FORBIDDEN);
	}

	protected void testGetAuthForPartner_whenWithBpRoleAuthRead_thenAccessGrantedViaHasOperationForPartner() {
		final String jeapToken = createJeapTokenForBpRoles(SEMANTIC_AUTH_READ_ROLE);
		assertHttpStatusWithTokenOnGetForPartner(semanticOperationAuthForPartnerPathTemplateSpec, jeapToken, PARTNER_ID, HttpStatus.OK);
	}

	protected void testGetAuthForPartner_whenWithoutBpRoleAuthRead_thenAccessDeniedViaHasOperationForPartner() {
		final String jeapToken = createJeapTokenForBpRoles(SEMANTIC_AUTH_READ_ROLE);
		assertHttpStatusWithTokenOnGetForPartner(semanticOperationAuthForPartnerPathTemplateSpec, jeapToken, OTHER_PARTNER_ID, HttpStatus.FORBIDDEN);
	}

	protected void testGetAuth_whenWithUserRole_thenAccessGrantedViaHasOperationForAllPartners() {
		final String jeapToken = createJeapTokenWithUserRoles(SEMANTIC_AUTH_READ_ROLE);
		assertHttpStatusWithTokenOnGet(semanticOperationAllPartnersAuthPathSpec, jeapToken, HttpStatus.OK);
	}

	protected void testGetAuth_whenOnlyWithBpRole_thenAccessDeniedViaHasOperationForAllPartners() {
		final String jeapToken = createJeapTokenForBpRoles(SEMANTIC_AUTH_READ_ROLE);
		assertHttpStatusWithTokenOnGet(semanticOperationAllPartnersAuthPathSpec, jeapToken, HttpStatus.FORBIDDEN);
	}

	protected void testGetAuth_whenRoleExpressionContainsSeparatorCharacter_thenAccessDenied() {
		final String jeapToken = createJeapTokenWithUserRoles(SEMANTIC_AUTH_READ_ROLE);
		assertHttpStatusWithTokenOnGet(semanticSeparatorValidationAuthPathSpec, jeapToken, HttpStatus.FORBIDDEN);
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
