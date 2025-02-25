package ch.admin.bit.jeap.security.user;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public interface JeapCurrentUser {

    String getSubject();

    default String getName() {
        return null;
    }

    default String getPreferredUsername() {
        return null;
    }

    default String getFamilyName() {
        return null;
    }

    default String getGivenName() {
        return null;
    }

    default String getLocale() {
        return null;
    }

    default String getAuthenticationContextClassReference() {
        return null;
    }

    default List<String> getAuthenticationMethodsReferences() {
        return Collections.emptyList();
    }

    default String getAdminDirUid() {
        return null;
    }

    default String getUserExtId() {
        return null;
    }

    default Set<String> getUserRoles() {
        return Collections.emptySet();
    }

    default Map<String, Set<String>> getBusinessPartnerRoles() {
        return Collections.emptyMap();
    }

    default String getPamsLoginLevel() {
        return null;
    }
}
