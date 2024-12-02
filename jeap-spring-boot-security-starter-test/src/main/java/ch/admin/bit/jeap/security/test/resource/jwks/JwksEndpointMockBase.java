package ch.admin.bit.jeap.security.test.resource.jwks;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JwksEndpointMockBase {

    protected static final String CONTEXT_PATH = "/.well-known";
    protected static final String JWKS_PATH = CONTEXT_PATH + "/jwks.json";

    private List<JWK> jwks;

    public static String getContextPath() {
        return CONTEXT_PATH;
    }

    public static String getJwksPath() {
        return JWKS_PATH;
    }

    public JwksEndpointMockBase(Set<JWK> jwks) {
        this.jwks = new ArrayList<>(jwks);
    }

    public Map<String, Object> getJwksJson() {
        return new JWKSet(jwks).toJSONObject(); // does not expose private keys that might be in the jwks
    }

}
