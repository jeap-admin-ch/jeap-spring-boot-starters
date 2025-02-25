package ch.admin.bit.jeap.security.user;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JeapCurrentUserDto implements JeapCurrentUser {

    String subject;

    String name;

    String preferredUsername;

    String familyName;

    String givenName;

    String locale;

    String authenticationContextClassReference;

    List<String> authenticationMethodsReferences;

    String adminDirUid;

    String userExtId;

    Set<String> userRoles;

    Map<String, Set<String>> businessPartnerRoles;

    String pamsLoginLevel;

    protected JeapCurrentUserDto(JeapAuthenticationToken authenticationToken) {
        this.subject = authenticationToken.getTokenSubject();
        this.name = authenticationToken.getTokenName();
        this.preferredUsername = authenticationToken.getPreferredUsername();
        this.familyName = authenticationToken.getTokenFamilyName();
        this.givenName = authenticationToken.getTokenGivenName();
        this.locale = authenticationToken.getToken().getClaim("locale");
        this.authenticationContextClassReference = authenticationToken.getToken().getClaim("acr");
        this.authenticationMethodsReferences = authenticationToken.getToken().getClaim("amr");
        this.adminDirUid = authenticationToken.getToken().getClaim("admin_dir_uid");
        this.userExtId = authenticationToken.getTokenExtId();
        this.userRoles = authenticationToken.getUserRoles();
        this.businessPartnerRoles = authenticationToken.getBusinessPartnerRoles();
        this.pamsLoginLevel = authenticationToken.getToken().getClaim("login_level");
    }

    public JeapCurrentUserDto(JeapCurrentUser jeapCurrentUser) {
        this.subject = jeapCurrentUser.getSubject();
        this.name = jeapCurrentUser.getName();
        this.preferredUsername = jeapCurrentUser.getPreferredUsername();
        this.familyName = jeapCurrentUser.getFamilyName();
        this.givenName = jeapCurrentUser.getGivenName();
        this.locale = jeapCurrentUser.getLocale();
        this.authenticationContextClassReference = jeapCurrentUser.getAuthenticationContextClassReference();
        this.authenticationMethodsReferences = jeapCurrentUser.getAuthenticationMethodsReferences();
        this.adminDirUid = jeapCurrentUser.getAdminDirUid();
        this.userExtId = jeapCurrentUser.getUserExtId();
        this.userRoles = jeapCurrentUser.getUserRoles();
        this.businessPartnerRoles = jeapCurrentUser.getBusinessPartnerRoles();
        this.pamsLoginLevel = jeapCurrentUser.getPamsLoginLevel();
    }

}
