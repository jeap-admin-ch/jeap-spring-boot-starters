package ch.admin.bit.jeap.security.user;

import ch.admin.bit.jeap.security.resource.authentication.ReactiveSimpleAuthorization;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.ReactiveSemanticAuthorization;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Optional;

@RestController
@RequestMapping("${jeap.security.oauth2.current-user-endpoint.path:/api/current-user}")
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class JeapCurrentUserWebFluxController {

    private final JeapCurrentUserMapper jeapCurrentUserMapper;

    private final Optional<ReactiveSemanticAuthorization> semanticAuthorization;

    private final Optional<ReactiveSimpleAuthorization> simpleAuthorization;

    @GetMapping()
    public Mono<JeapCurrentUser> getCurrentUser() {
        if (semanticAuthorization.isPresent()) {
            return semanticAuthorization.get().getAuthenticationToken().map(jeapCurrentUserMapper::mapCurrentUser);
        } else if (simpleAuthorization.isPresent()) {
            return simpleAuthorization.get().getJeapAuthenticationToken().map(jeapCurrentUserMapper::mapCurrentUser);
        }
        throw new IllegalStateException("Cannot determine the current authentication: No ReactiveSemanticAuthorization or ReactiveSimpleAuthorization available");
    }
}
