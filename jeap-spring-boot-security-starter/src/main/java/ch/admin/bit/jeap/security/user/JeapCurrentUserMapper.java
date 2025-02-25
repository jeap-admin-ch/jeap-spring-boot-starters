package ch.admin.bit.jeap.security.user;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JeapCurrentUserMapper {

    private final JeapCurrentUserCustomizer<? extends JeapCurrentUser> jeapCurrentUserCustomizer;

    public JeapCurrentUserMapper(Optional<JeapCurrentUserCustomizer<? extends JeapCurrentUser>> jeapCustomCurrentUserMapper) {
        this.jeapCurrentUserCustomizer = jeapCustomCurrentUserMapper.orElse(null);
    }

    public JeapCurrentUser mapCurrentUser(JeapAuthenticationToken authenticationToken) {
        JeapCurrentUserDto jeapCurrentUser = new JeapCurrentUserDto(authenticationToken);
        if (jeapCurrentUserCustomizer != null) {
            return jeapCurrentUserCustomizer.customize(jeapCurrentUser);
        }
        return jeapCurrentUser;
    }

}
