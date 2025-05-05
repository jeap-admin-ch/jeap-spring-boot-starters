package ch.admin.bit.jeap.security.resource.introspection;

public class JeapIntrospectionInvalidTokenException extends JeapIntrospectionException{

    private static final String MESSAGE = "The token is invalid";

    public JeapIntrospectionInvalidTokenException() {
        super(MESSAGE);
    }

    public JeapIntrospectionInvalidTokenException(Throwable cause) {
        super(MESSAGE, cause);
    }

}
