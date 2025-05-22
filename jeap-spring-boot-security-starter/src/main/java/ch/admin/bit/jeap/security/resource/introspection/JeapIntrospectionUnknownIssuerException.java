package ch.admin.bit.jeap.security.resource.introspection;

public class JeapIntrospectionUnknownIssuerException extends JeapIntrospectionException {

    private static final String BASE_MESSAGE = "The issuer '%s' is unknown: ";

    public JeapIntrospectionUnknownIssuerException(String issuer, String details) {
        super(BASE_MESSAGE.formatted(issuer) + details);
    }

}
