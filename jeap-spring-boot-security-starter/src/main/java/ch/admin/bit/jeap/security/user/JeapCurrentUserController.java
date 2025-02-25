package ch.admin.bit.jeap.security.user;

import ch.admin.bit.jeap.security.resource.authentication.ServletSimpleAuthorization;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.ServletSemanticAuthorization;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("${jeap.security.oauth2.current-user-endpoint.path:/api/current-user}")
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class JeapCurrentUserController {

    private final JeapCurrentUserMapper jeapCurrentUserMapper;

    private final Optional<ServletSemanticAuthorization> semanticAuthorization;

    private final Optional<ServletSimpleAuthorization> simpleAuthorization;

    @GetMapping()
    public JeapCurrentUser getCurrentUser() {
        if (semanticAuthorization.isPresent()) {
            return jeapCurrentUserMapper.mapCurrentUser(semanticAuthorization.get().getAuthenticationToken());
        } else if (simpleAuthorization.isPresent()) {
            return jeapCurrentUserMapper.mapCurrentUser(simpleAuthorization.get().getJeapAuthenticationToken());
        }
        throw new IllegalStateException("Cannot determine the current authentication: No ServletSemanticAuthorization or ServletSimpleAuthorization available");
    }
}