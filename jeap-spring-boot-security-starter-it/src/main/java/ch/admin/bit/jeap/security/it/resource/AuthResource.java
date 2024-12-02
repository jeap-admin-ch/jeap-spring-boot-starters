package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.authentication.ReactiveSimpleAuthorization;
import ch.admin.bit.jeap.security.resource.authentication.ServletSimpleAuthorization;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.ReactiveSemanticAuthorization;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.ServletSemanticAuthorization;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthResource {

    private final Optional<ReactiveSemanticAuthorization> reactiveSemanticAuthorization;
    private final Optional<ServletSemanticAuthorization> servletSemanticAuthorization;
    private final Optional<ServletSimpleAuthorization> servletSimpleAuthorization;
    private final Optional<ReactiveSimpleAuthorization> reactiveSimpleAuthorization;

    @GetMapping("/api/semantic/auth")
    @PreAuthorize("hasRole('auth', 'read')")
    public Mono<Auth> getAuthProtectedBySemanticRole() {
        return getJeapAuthenticationTokenMono().
                map(this::createAuthFromToken);
    }

    @GetMapping("/api/semantic-programmatic/auth")
    public Mono<Auth> getAuthProtectedBySemanticRoleProgrammatic() {
        return reactiveSemanticAuthorization.map(rsa -> rsa.getSemanticRoleRepository()
                        .map(repo -> repo.hasRole("auth", "read")))
               .or(() -> servletSemanticAuthorization.map(ssa ->  Mono.just(ssa.hasRole("auth", "read"))))
               .orElseThrow(() -> new IllegalStateException("No semantic authorization bean present."))
               .flatMap(hasRole -> hasRole ? getAuthProtectedBySemanticRole() :
                       Mono.error(new AccessDeniedException("Missing role 'auth_read'")));
    }

    @GetMapping("/api/semantic/{partnerId}/auth")
    @PreAuthorize("hasRoleForPartner('auth', 'read', #partnerId)")
    public Mono<Auth> getAuthForPartnerProtectedBySemanticRole(@PathVariable("partnerId") String partnerId) {
        return getJeapAuthenticationTokenMono().
                map(token -> createAuthFromTokenForPartner(partnerId, token));
    }

    @GetMapping("/api/semantic-programmatic/{partnerId}/auth")
    public Mono<Auth> getAuthForPartnerProtectedBySemanticRoleProgrammatic(@PathVariable("partnerId") String partnerId) {
        return reactiveSemanticAuthorization.map(rsa -> rsa.getSemanticRoleRepository()
                    .map(repo -> repo.hasRoleForPartner("auth", "read", partnerId)))
               .or(() -> servletSemanticAuthorization
                     .map(ssa -> Mono.just(ssa.hasRoleForPartner("auth", "read", partnerId))))
               .orElseThrow(() -> new IllegalStateException("No semantic authorization bean present."))
               .flatMap(hasRole -> hasRole ? getAuthForPartnerProtectedBySemanticRole(partnerId) :
                       Mono.error(new AccessDeniedException("Missing role 'auth_read'  for partner " + partnerId)));
    }

    @GetMapping("/api/simple/auth")
    @PreAuthorize("hasRole('authentication:read')")
    public Mono<Auth> getAuthProtectedBySimpleRole() {
        return getJeapAuthenticationTokenMono().
                map(this::createAuthFromToken);
    }

    @GetMapping("/api/simple-programmatic/auth")
    public Mono<Auth> getAuthProtectedBySimpleRoleProgrammatic() {
        return reactiveSimpleAuthorization.map(rsa -> rsa.getSimpleRoleRepository()
                        .map(repo -> repo.hasRole("authentication:read")))
                .or(() -> servletSimpleAuthorization.map(ssa ->  Mono.just(ssa.hasRole("authentication:read"))))
                .orElseThrow(() -> new IllegalStateException("No simple authorization bean present."))
                .flatMap(hasRole -> hasRole ? getAuthProtectedBySimpleRole() :
                        Mono.error(new AccessDeniedException("Missing role 'authentication:read'")));
    }

    @GetMapping("/api/simple/{partnerId}/auth")
    @PreAuthorize("hasRoleForPartner('authentication:read', #partnerId)")
    public Mono<Auth> getAuthForPartnerProtectedBySimpleRole(@PathVariable("partnerId") String partnerId) {
        return getJeapAuthenticationTokenMono().
                map(token -> createAuthFromTokenForPartner(partnerId, token));
    }

    @GetMapping("/api/simple-programmatic/{partnerId}/auth")
    public Mono<Auth> getAuthForPartnerProtectedBySimpleRoleProgrammatic(@PathVariable("partnerId") String partnerId) {
        return reactiveSimpleAuthorization.map(rsa -> rsa.getSimpleRoleRepository()
                        .map(repo -> repo.hasRoleForPartner("authentication:read", partnerId)))
                .or(() -> servletSimpleAuthorization
                        .map(ssa -> Mono.just(ssa.hasRoleForPartner("authentication:read", partnerId))))
                .orElseThrow(() -> new IllegalStateException("No simple authorization bean present."))
                .flatMap(hasRole -> hasRole ? getAuthForPartnerProtectedBySimpleRole(partnerId) :
                        Mono.error(new AccessDeniedException("Missing role 'authentication:read' for partner " + partnerId)));
    }

    @GetMapping("/api/authorities/auth")
    @PreAuthorize("hasAuthority('resource:write')")
    public Mono<Auth> getAuthProtectedByAuthorities() {
        return getJeapAuthenticationTokenMono().
                map(this::createAuthFromToken);
    }

    @GetMapping("/api/authorities-programmatic/auth")
    public Mono<Auth> getAuthProtectedByAuthoritiesProgrammatic() {
        return getJeapAuthenticationTokenMono()
                .map(authToken -> authToken.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).anyMatch("resource:write"::equals))
                .flatMap(hasAuthority -> hasAuthority ? getAuthProtectedByAuthorities() :
                        Mono.error(new AccessDeniedException("Missing authority 'resource:write' ")));
    }

    @GetMapping("/api/error")
    @PreAuthorize("hasRole('auth', 'read')")
    public Mono<Auth> getAuthThrowingException() {
        throw new IllegalStateException("Oops");
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

    private Mono<JeapAuthenticationToken> getJeapAuthenticationTokenMono() {
        if (reactiveSemanticAuthorization.isPresent()) {
            // running on webflux stack with semantic authorization -> fetch token from reactive semantic role authorization
            return reactiveSemanticAuthorization.get().getAuthenticationToken();
        } else if (servletSemanticAuthorization.isPresent()) {
            // running on webmvc stack with semantic authorization -> fetch token from semantic role authorization
            return Mono.just(servletSemanticAuthorization.get().getAuthenticationToken());
        } else if (servletSimpleAuthorization.isPresent()) {
            // running on webmvc stack with simple authorization -> fetch token from simple role authorization
            return Mono.just(servletSimpleAuthorization.get().getJeapAuthenticationToken());
        } else if (reactiveSimpleAuthorization.isPresent()) {
            return reactiveSimpleAuthorization.get().getJeapAuthenticationToken();
        } else {
            throw new IllegalStateException("No authorization bean present.");
        }
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Access denied")
    @ExceptionHandler(AccessDeniedException.class)
    private void accessDenied() {
        // return http staus 403
    }

}
