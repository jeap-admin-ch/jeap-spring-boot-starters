package ch.admin.bit.jeap.security.resource.properties;

/**
 * Controls how the resource server treats access tokens in the USER and SYS authentication contexts that do not
 * specify an audience, i.e. tokens whose 'aud' claim is missing or empty. Access tokens in the B2B context are never
 * audience-checked, as the B2B gateway cannot restrict the audience of the tokens it issues.
 */
public enum StrictAudienceValidationMode {

    /**
     * Legacy behaviour: a token without an audience is considered valid for every resource and therefore accepted.
     */
    OFF,

    /**
     * Strict behaviour: a token without an audience does not address this resource and is therefore rejected.
     */
    ON,

    /**
     * Migration aid: a token without an audience is accepted as in mode {@link #OFF}, but a warning identifying the
     * token is logged, as the token would be rejected in mode {@link #ON}. The warning is only logged for tokens that
     * pass all other validations, i.e. exactly for the tokens that are accepted in mode {@link #OFF}.
     */
    WARN

}
