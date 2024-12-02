package ch.admin.bit.jeap.security.test.resource.jwks;

import com.nimbusds.jose.jwk.JWK;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.util.Map;
import java.util.Set;

@RestController
public class ReactiveJwksEndpointMock extends JwksEndpointMockBase {

    public ReactiveJwksEndpointMock(Set<JWK> jwks) {
        super(jwks);
    }

    @GetMapping(JWKS_PATH)
    public Mono<Map<String, Object>> jwksJson() {
        return Mono.just(super.getJwksJson());
    }

}
