# Security starter

`jeap-spring-boot-security-starter` turns a service into a Spring Security OAuth2 **resource server**:
it validates the JWT bearer access token on every request, converts it into a `JeapAuthenticationToken`,
and lets the application authorize the request against the roles carried in that token. It is the
standard, mandated way to protect REST APIs in jEAP services — per the jEAP guidelines, *every*
REST endpoint must be authenticated and authorized, including endpoints only called internally.

Calling other secured services is covered by
[`jeap-spring-boot-security-client-starter`](jeap-spring-boot-security-client-starter.md); testing
protected endpoints is covered by
[`jeap-spring-boot-security-starter-test`](jeap-spring-boot-security-starter-test.md).

## Add it

```xml
<dependency>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-spring-boot-security-starter</artifactId>
</dependency>
```

The starter auto-configures the resource server, a default deny-all `SecurityFilterChain`, token
conversion and (optionally) introspection and the current-user endpoint. It targets the Spring WebMvc
stack. The WebFlux/reactive stack is no longer supported.

## Authentication

### Authentication contexts

Authorization always happens within a *context*. jEAP distinguishes three, each issued and validated
differently and exposed via `JeapAuthenticationToken.getJeapAuthenticationContext()` (parsed from the
`ctx` JWT claim):

| `JeapAuthenticationContext` | Meaning                                                                                                                                    |
|-----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| `USER`                      | A request by a human (natural person, employee, or business-partner user) via a UI or mobile app. The token lifetime is at most 5 minutes. |
| `SYS`                       | A service-to-service request made by an internal system using a technical user, typically via OAuth2 client credentials.                   |
| `B2B`                       | A request from an external partner system arriving through the B2B/API gateway, bound to one business partner via an API subscription.     |

A context can also be *propagated* horizontally: a service may forward the token it received to a
downstream service (see the [client starter](jeap-spring-boot-security-client-starter.md)).

### What the starter validates

For every incoming token the resource server enforces the jEAP token requirements:

- the signature algorithm is `RS256`/`RS512` — `alg=none` and weak algorithms are rejected;
- the `iss` (issuer) claim matches a configured authorization server, and the signing key fits that issuer;
- the `aud` (audience) claim matches the service's `resource-id` (or `spring.application.name`);
- the authentication context (`ctx`) of the token is allowed for that authorization server.

On success the `Authentication` placed in the `SecurityContext` is a `JeapAuthenticationToken`
(`ch.admin.bit.jeap.security.resource.token`), a `JwtAuthenticationToken` carrying the JWT claims plus
the parsed `userRoles` and `businessPartnerRoles` (see [Authorization](#authorization)).

### Minimal configuration

```yaml
jeap:
  security:
    oauth2:
      resourceserver:
        authorization-server:
          issuer: https://keycloak.example.ch/auth/realms/myrealm
```

`jwk-set-uri` defaults to `{issuer}/protocol/openid-connect/certs`. The default audience is
`spring.application.name`; set `resource-id` when tokens carry a restricted audience.

### Multiple authorization servers and the B2B gateway

Trust several issuers at once with `auth-servers[*]`, and configure a separate B2B gateway issuer
(which defaults to the `B2B` context):

```yaml
jeap:
  security:
    oauth2:
      resourceserver:
        auth-servers:
          - issuer: https://keycloak.example.ch/auth/realms/users
            authentication-contexts: [USER]
          - issuer: https://keycloak.example.ch/auth/realms/system
            authentication-contexts: [SYS]
        b2b-gateway:
          issuer: https://b2b-gw.example.ch
          jwk-set-uri: https://b2b-gw.example.ch/certs
```

### Customizing the filter chain

By default the starter protects all URIs with OAuth2 (plus CORS and CSRF). To expose a public API,
a Swagger UI behind basic auth, or a self-contained-system UI, declare your own `SecurityFilterChain`
bean with a higher precedence (lower `@Order` value) and a tight `RequestMatcher` — the first chain
whose matcher accepts the request wins, so keep custom matchers narrow. The starter's chains use
orders relative to `Ordered.LOWEST_PRECEDENCE` so applications can override them; use a dedicated
role (`hasRole(...)`, not just `fullyAuthenticated()`) for any custom authentication, and avoid
HTTP session management.

### Key properties

Prefix: `jeap.security.oauth2.resourceserver`.

| Property                                              | Default                                  | Description                                                                               |
|-------------------------------------------------------|------------------------------------------|-------------------------------------------------------------------------------------------|
| `resource-id`                                         | —                                        | Expected audience for tokens with a restricted audience                                   |
| `application-name`                                    | `${spring.application.name}`             | Default expected audience when `resource-id` is unset                                     |
| `system-name`                                         | —                                        | System name; **setting it activates semantic-role authorization**                         |
| `authorization-server.issuer`                         | —                                        | Token issuer (shortcut for a single auth server)                                          |
| `authorization-server.jwk-set-uri`                    | `{issuer}/protocol/openid-connect/certs` | JWKS endpoint for signing certificates                                                    |
| `authorization-server.authentication-contexts`        | `[USER, SYS]`                            | Allowed contexts for this auth server                                                     |
| `authorization-server.jwks-connect-timeout-in-millis` | `15000`                                  | Connect timeout for fetching the JWKS                                                     |
| `authorization-server.jwks-read-timeout-in-millis`    | `15000`                                  | Read timeout for fetching the JWKS                                                        |
| `b2b-gateway.issuer` / `b2b-gateway.jwk-set-uri`      | —                                        | B2B gateway issuer and JWKS endpoint (both required)                                      |
| `b2b-gateway.authentication-contexts`                 | `[B2B]`                                  | Allowed contexts for the B2B gateway                                                      |
| `auth-servers[*]`                                     | —                                        | List of additional auth servers (same fields as `authorization-server`)                   |
| `introspection.mode`                                  | —                                        | Resource-level introspection mode (`NONE`, `EXPLICIT`, `ALWAYS`, `LIGHTWEIGHT`, `CUSTOM`) |
| `log-user-access`                                     | `false`                                  | Register a filter that logs each authenticated user's access                              |
| `log.authentication-failure.enabled`                  | `false`                                  | Log failed authentications (invalid/missing bearer tokens)                                |
| `log.access-denied.enabled`                           | `false`                                  | Log authorization failures (authenticated but not permitted)                              |
| `log.access-denied.debug`                             | `false`                                  | Add debug detail to denied-request logs (needs the line above)                            |

### Current-user endpoint

When enabled, the starter exposes the authenticated user's profile for a frontend to read, serialized
from the `JeapCurrentUser` view (subject, name, username, roles, business-partner roles, locale, ...):

| Property                                             | Default             | Description                      |
|------------------------------------------------------|---------------------|----------------------------------|
| `jeap.security.oauth2.current-user-endpoint.enabled` | `false`             | Enable the current-user endpoint |
| `jeap.security.oauth2.current-user-endpoint.path`    | `/api/current-user` | Path of the `GET` endpoint       |

## Authorization

Once a request is authenticated, the starter authorizes it against the roles carried in the JWT. jEAP
distinguishes **functional** authorization (may the user invoke this operation? — checked on the REST
layer before the call) from **data** authorization (may the user access *these* data? — often checked
after the data is loaded, e.g. against a business partner or tenant). Authorization is a property of
the REST API and must be performed on the REST layer.

Roles come from two JWT claims:

- `userroles` — roles the user holds independently of any business partner (e.g. admins, technical users);
- `bproles` — roles the user holds *for a specific business partner*, a `{ partnerId: [roles] }` map.

The starter supports two role models, *simple* and *semantic* (plus application-defined *authorities*,
derived in your own code from coarse roles — out of scope here).

### Choosing a model

| Model    | Activation                                       | Role shape                                       |
|----------|--------------------------------------------------|--------------------------------------------------|
| Simple   | `resourceserver.system-name` **must NOT be set** | Opaque strings, e.g. `admin`                     |
| Semantic | `resourceserver.system-name` **must be set**     | Structured `system_%tenant_@resource_#operation` |

Semantic roles are simple roles whose parts have a defined meaning: `system` (the application that
authorizes against the role; mandatory), `tenant` (which mandant may exercise it), `resource` (the
resource type), and `operation`. Parts are separated by special characters so each is unambiguous;
omitting a part acts as a wildcard for it (any part except `system` may be omitted). Two syntaxes
exist — the standard `system_%tenant_@resource_#operation` and an eIAM-compatible alternative
`system_:tenant_@resource_!operation` (eIAM disallows `%` and `#` in role names). `SemanticApplicationRole`
parses both automatically.

### Query methods

Both models expose the same query methods; semantic roles take role components instead of a single
role string:

| Method (simple)                    | Method (semantic)                                 | Meaning                                                     |
|------------------------------------|---------------------------------------------------|-------------------------------------------------------------|
| `hasRole(role)`                    | `hasRole(resource, operation)`                    | Has the role for at least one partner or independent of one |
| `hasRoleForPartner(role, partner)` | `hasRoleForPartner(resource, operation, partner)` | Has the role for the given business partner (`bproles`)     |
| `hasRoleForAllPartners(role)`      | `hasRoleForAllPartners(resource, operation)`      | Has the role independent of any partner (`userroles`)       |
| `getPartnersForRole(role)`         | `getPartnersForRole(resource, operation)`         | Which partners the user holds the role for                  |

Semantic queries may supply fewer components: `hasOperation(operation)`, `hasRole(resource, operation)`
or `hasRole(tenant, resource, operation)` — only the supplied components are checked.

### Declarative authorization

The query methods are available in the SpEL of Spring Security's `@PreAuthorize` / `@PostAuthorize`:

```java
// simple roles
@PreAuthorize("hasRole('admin')")
public Data getData() { ... }

@PreAuthorize("hasRoleForPartner('reader', #partnerId)")
public Partner getPartner(String partnerId) { ... }
```

```java
// semantic roles
@PreAuthorize("hasRole('anmeldung', 'lesen')")
public Data getData() { ... }

@PreAuthorize("hasRoleForPartner('anmeldung', 'lesen', #partnerId)")
public Partner getPartner(String partnerId) { ... }
```

> IntelliJ does not recognise these custom methods, so there is no auto-completion inside the SpEL string.

### Programmatic authorization

Some checks need loaded data first. Inject the matching authorization bean and call the same methods
in code, throwing `AccessDeniedException` on failure:

```java
import ch.admin.bit.jeap.security.resource.authentication.ServletSimpleAuthorization;

@RequiredArgsConstructor
class PartnerService {
    private final ServletSimpleAuthorization jeapAuthorization;

    Partner findPartner(String partnerId) {
        Partner partner = repository.load(partnerId);
        if (!jeapAuthorization.hasRoleForPartner("reader", partner.getPartnerId())) {
            throw new AccessDeniedException("Missing role for partner " + partner.getPartnerId());
        }
        return partner;
    }
}
```

For semantic roles inject
`ch.admin.bit.jeap.security.resource.semanticAuthentication.ServletSemanticAuthorization` instead
(only created when `system-name` is set). It additionally offers `hasOperation(...)`,
`getAllRolesForOperation(...)` and partner-scoped variants.

### The authentication token

`JeapAuthenticationToken` (`ch.admin.bit.jeap.security.resource.token`) is the `Authentication` in the
`SecurityContext`. Besides `getUserRoles()` and `getBusinessPartnerRoles()` it exposes claims via
`getClientId()`, `getTokenSubject()`, `getPreferredUsername()`, `getTokenName()`,
`getTokenGivenName()`, `getTokenFamilyName()`, `getTokenExtId()`, `getTokenLocale()`,
`getAdminDirUID()` and `getJeapAuthenticationContext()`. A read-only view of the user profile is also
available through the `JeapCurrentUser` interface (`ch.admin.bit.jeap.security.user`).

## Related

- [jeap-spring-boot-security-client-starter](jeap-spring-boot-security-client-starter.md)
- [jeap-spring-boot-security-starter-test](jeap-spring-boot-security-starter-test.md)
- [Configuration property reference](configuration.md)
- [jeap-spring-boot-starters](../README.md)
