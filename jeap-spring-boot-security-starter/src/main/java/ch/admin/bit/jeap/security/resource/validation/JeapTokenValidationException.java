package ch.admin.bit.jeap.security.resource.validation;

import org.springframework.security.core.AuthenticationException;

class JeapTokenValidationException extends AuthenticationException {

    JeapTokenValidationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    JeapTokenValidationException(String msg) {
        super(msg);
    }
}
