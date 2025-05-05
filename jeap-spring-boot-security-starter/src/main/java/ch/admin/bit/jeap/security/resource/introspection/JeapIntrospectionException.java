package ch.admin.bit.jeap.security.resource.introspection;

public class JeapIntrospectionException extends RuntimeException {

    public JeapIntrospectionException(String message) {
        super(message);
    }

    public JeapIntrospectionException(String message, Throwable cause) {
        super(message, cause);
    }

}
