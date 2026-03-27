package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.authentication.ServletSimpleAuthorization;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.ServletSemanticAuthorization;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthResource {

    private final Optional<ServletSemanticAuthorization> servletSemanticAuthorization;
    private final Optional<ServletSimpleAuthorization> servletSimpleAuthorization;

    @GetMapping("/api/semantic/auth")
    @PreAuthorize("hasRole('auth', 'read')")
    public Auth getAuthProtectedBySemanticRole() {
        return createAuthFromToken(getJeapAuthenticationToken());
    }

    @GetMapping("/api/semantic-programmatic/auth")
    public Auth getAuthProtectedBySemanticRoleProgrammatic() {
        boolean hasRole = servletSemanticAuthorization
                .map(ssa -> ssa.hasRole("auth", "read"))
                .orElseThrow(() -> new IllegalStateException("No semantic authorization bean present."));
        if (!hasRole) throw new AccessDeniedException("Missing role 'auth_read'");
        return getAuthProtectedBySemanticRole();
    }

    @GetMapping("/api/semantic/{partnerId}/auth")
    @PreAuthorize("hasRoleForPartner('auth', 'read', #partnerId)")
    public Auth getAuthForPartnerProtectedBySemanticRole(@PathVariable("partnerId") String partnerId) {
        return createAuthFromTokenForPartner(partnerId, getJeapAuthenticationToken());
    }

    @GetMapping("/api/semantic-programmatic/{partnerId}/auth")
    public Auth getAuthForPartnerProtectedBySemanticRoleProgrammatic(@PathVariable("partnerId") String partnerId) {
        boolean hasRole = servletSemanticAuthorization
                .map(ssa -> ssa.hasRoleForPartner("auth", "read", partnerId))
                .orElseThrow(() -> new IllegalStateException("No semantic authorization bean present."));
        if (!hasRole) throw new AccessDeniedException("Missing role 'auth_read' for partner " + partnerId);
        return getAuthForPartnerProtectedBySemanticRole(partnerId);
    }

    @GetMapping("/api/semantic-operation/auth")
    @PreAuthorize("hasOperation('read')")
    public Auth getAuthProtectedBySemanticOperation() {
        return createAuthFromToken(getJeapAuthenticationToken());
    }

    @GetMapping("/api/semantic-operation/{partnerId}/auth")
    @PreAuthorize("hasOperationForPartner('read', #partnerId)")
    public Auth getAuthForPartnerProtectedBySemanticOperation(@PathVariable("partnerId") String partnerId) {
        return createAuthFromTokenForPartner(partnerId, getJeapAuthenticationToken());
    }

    @GetMapping("/api/semantic-operation-all-partners/auth")
    @PreAuthorize("hasOperationForAllPartners('read')")
    public Auth getAuthProtectedBySemanticOperationForAllPartners() {
        return createAuthFromToken(getJeapAuthenticationToken());
    }

    @GetMapping("/api/semantic-separator-validation/auth")
    @PreAuthorize("hasRole('jme_@auth_#read', 'read')")
    public Auth getAuthProtectedBySemanticRoleWithSeparator() {
        return createAuthFromToken(getJeapAuthenticationToken());
    }

    @GetMapping("/api/simple/auth")
    @PreAuthorize("hasRole('authentication:read')")
    public Auth getAuthProtectedBySimpleRole() {
        return createAuthFromToken(getJeapAuthenticationToken());
    }

    @GetMapping("/api/simple-programmatic/auth")
    public Auth getAuthProtectedBySimpleRoleProgrammatic() {
        boolean hasRole = servletSimpleAuthorization
                .map(ssa -> ssa.hasRole("authentication:read"))
                .orElseThrow(() -> new IllegalStateException("No simple authorization bean present."));
        if (!hasRole) throw new AccessDeniedException("Missing role 'authentication:read'");
        return getAuthProtectedBySimpleRole();
    }

    @GetMapping("/api/simple/{partnerId}/auth")
    @PreAuthorize("hasRoleForPartner('authentication:read', #partnerId)")
    public Auth getAuthForPartnerProtectedBySimpleRole(@PathVariable("partnerId") String partnerId) {
        return createAuthFromTokenForPartner(partnerId, getJeapAuthenticationToken());
    }

    @GetMapping("/api/simple-programmatic/{partnerId}/auth")
    public Auth getAuthForPartnerProtectedBySimpleRoleProgrammatic(@PathVariable("partnerId") String partnerId) {
        boolean hasRole = servletSimpleAuthorization
                .map(ssa -> ssa.hasRoleForPartner("authentication:read", partnerId))
                .orElseThrow(() -> new IllegalStateException("No simple authorization bean present."));
        if (!hasRole) throw new AccessDeniedException("Missing role 'authentication:read' for partner " + partnerId);
        return getAuthForPartnerProtectedBySimpleRole(partnerId);
    }

    @GetMapping("/api/authorities/auth")
    @PreAuthorize("hasAuthority('resource:write')")
    public Auth getAuthProtectedByAuthorities() {
        return createAuthFromToken(getJeapAuthenticationToken());
    }

    @GetMapping("/api/authorities-programmatic/auth")
    public Auth getAuthProtectedByAuthoritiesProgrammatic() {
        JeapAuthenticationToken token = getJeapAuthenticationToken();
        boolean hasAuthority = token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("resource:write"::equals);
        if (!hasAuthority) throw new AccessDeniedException("Missing authority 'resource:write'");
        return getAuthProtectedByAuthorities();
    }

    @GetMapping("/api/error")
    @PreAuthorize("hasRole('auth', 'read')")
    public Auth getAuthThrowingException() {
        throw new IllegalStateException("Oops");
    }

    private JeapAuthenticationToken getJeapAuthenticationToken() {
        if (servletSemanticAuthorization.isPresent()) {
            return servletSemanticAuthorization.get().getAuthenticationToken();
        } else if (servletSimpleAuthorization.isPresent()) {
            return servletSimpleAuthorization.get().getJeapAuthenticationToken();
        }
        throw new IllegalStateException("No authorization bean present.");
    }

    private Auth createAuthFromToken(JeapAuthenticationToken token) {
        return createAuthWithUserInfoFrom(token)
                .ctx(token.getJeapAuthenticationContext().name())
                .userroles(token.getUserRoles())
                .bproles(token.getBusinessPartnerRoles())
                .authorities(token.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()))
                .build();
    }

    private Auth createAuthFromTokenForPartner(String partnerId, JeapAuthenticationToken token) {
        return createAuthWithUserInfoFrom(token)
                .ctx(token.getJeapAuthenticationContext().name())
                .userroles(token.getUserRoles())
                .bproles(Map.of(partnerId, token.getBusinessPartnerRoles().getOrDefault(partnerId, emptySet())))
                .build();
    }

    private Auth.AuthBuilder createAuthWithUserInfoFrom(JeapAuthenticationToken token) {
        return Auth.builder()
                .subject(token.getTokenSubject())
                .locale(token.getTokenLocale())
                .extId(token.getTokenExtId())
                .adminDirUID(token.getAdminDirUID())
                .familyName(token.getTokenFamilyName())
                .givenName(token.getTokenGivenName())
                .name(token.getTokenName())
                .preferredUsername(token.getPreferredUsername());
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Access denied")
    @ExceptionHandler(AccessDeniedException.class)
    private void accessDenied() {
        // return http status 403
    }
}
