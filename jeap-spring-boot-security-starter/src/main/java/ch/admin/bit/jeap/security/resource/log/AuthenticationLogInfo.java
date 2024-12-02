package ch.admin.bit.jeap.security.resource.log;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class AuthenticationLogInfo {

    private final String authenticationLogInfo;

    static AuthenticationLogInfo from(Object principal) {
        return new AuthenticationLogInfo(getAuthenticationLogInfo(principal));
    }

    @Override
    public String toString() {
        return authenticationLogInfo;
    }

    private static String getAuthenticationLogInfo(Object principal) {
        if (principal instanceof JeapAuthenticationToken) {
            JeapAuthenticationToken jeapAuthentication = (JeapAuthenticationToken) principal;
            return jeapAuthentication.toString();
        }
        else {
            return "no jeap authentication detected";
        }
    }

}
