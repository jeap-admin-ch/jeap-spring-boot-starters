package ch.admin.bit.jeap.security.test.resource.jwks;

import com.nimbusds.jose.jwk.JWK;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.Set;

@RestController
public class ServletJwksEndpointMock extends JwksEndpointMockBase {

    public ServletJwksEndpointMock(Set<JWK> jwks) {
        super(jwks);
    }

    @GetMapping(JWKS_PATH)
    public Map<String, Object> jwksJson() {
        return super.getJwksJson();
    }

}
