package ch.admin.bit.jeap.security.it.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Auth {
    private String subject;
    private String ctx;
    private Set<String> userroles;
    private Map<String, Set<String>> bproles;
    private String locale;
    private String extId;
    private String familyName;
    private String givenName;
    private String name;
    private String preferredUsername;
    private String adminDirUID;
    private Collection<String> authorities;
}
