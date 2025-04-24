package ch.admin.bit.jeap.security.resource.introspection;

import org.springframework.security.oauth2.jwt.Jwt;

public class AlwaysTokenIntrospectionCondition implements JeapJwtIntrospectionCondition {
    @Override
    public boolean needsIntrospection(Jwt jwt) {
        return true;
    }
}
