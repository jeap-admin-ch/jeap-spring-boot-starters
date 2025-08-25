package ch.admin.bit.jeap.security.it.bearertoken;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Documented
public @interface BearerTokenUrl {
}
