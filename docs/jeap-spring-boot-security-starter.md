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
- the `aud` (audience) claim contains the service's `resource-id`,  for details see
  [Audience validation](#audience-validation)
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

`jwk-set-uri` defaults to `{issuer}/protocol/openid-connect/certs`. Set `resource-id` when tokens carry
a restricted audience. The default audience is`spring.application.name`. See
[Audience validation](#audience-validation) for how tokens without an `aud` claim are treated.

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

### Audience validation

The expected audience of a token is the service's `resource-id`, defaulting to `spring.application.name`.
Tokens in the `USER` and `SYS` contexts whose `aud` claim does not contain that audience are rejected.
Tokens in the `B2B` context are never audience-checked, because the B2B gateway cannot restrict the
audience of the tokens it issues.

`strict-audience-validation` controls how tokens *without* an `aud` claim (missing or empty) are treated in the `USER`
and `SYS` contexts:

| Mode   | Token without `aud` claim                                                                       |
|--------|-------------------------------------------------------------------------------------------------|
| `off`  | Accepted — the token is considered valid for every resource (legacy behaviour, current default) |
| `warn` | Accepted, but a warning identifying the token (issuer, subject, client id, context) is logged   |
| `on`   | Rejected — a token that does not address this resource is not valid for it                      |

```yaml
jeap:
  security:
    oauth2:
      resourceserver:
        strict-audience-validation: on
```

Strict audience validation (`on`) will become the default in a future release. To migrate, switch to
`warn` first and search the logs for `strict-audience-validation=warn` to find the clients that still
request tokens without an audience; once no warnings show up anymore, switch to `on`.

### Token introspection

With an `introspection.mode` set on the resource server level, every trusted auth server needs an
`introspection` configuration (or `introspection.mode: NONE` to exclude it). The introspection endpoint
defaults to `{issuer}/protocol/openid-connect/token/introspect` and the introspection client id defaults
to the `resource-id` (Keycloak requires the introspection client id to be identical to the resource id.
Therefore, usually only the `client-secret` has to be configured:

```yaml
jeap:
  security:
    oauth2:
      resourceserver:
        introspection:
          mode: always
        authorization-server:
          issuer: https://keycloak.example.ch/auth/realms/myrealm
          introspection:
            client-secret: ${INTROSPECTION_CLIENT_SECRET}
```

With `introspection.mode: custom`, the application must provide exactly one `JeapJwtIntrospectionCondition`
bean deciding per token whether it needs to be introspected; with any other mode, a custom condition bean
fails the application startup, as it would not be used.

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

| Property                                                       | Default                                             | Description                                                                                                                         |
|----------------------------------------------------------------|-----------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `resource-id`                                                  | `${spring.application.name}`                        | Id of this resource, checked against the `aud` claim of access tokens                                                               |
| `strict-audience-validation`                                   | `off`                                               | `USER`/`SYS` tokens without `aud` claim: `off` accepts, `warn` accepts and logs, `on` rejects                                       |
| `system-name`                                                  | —                                                   | System name; **setting it activates semantic-role authorization**                                                                   |
| `introspection.mode`                                           | —                                                   | Resource-level introspection mode (`NONE`, `EXPLICIT`, `ALWAYS`, `LIGHTWEIGHT`, `CUSTOM`)                                           |
| `authorization-server.issuer`                                  | —                                                   | Token issuer (shortcut for a single auth server, required)                                                                          |
| `authorization-server.jwk-set-uri`                             | `{issuer}/protocol/openid-connect/certs`            | JWKS endpoint for signing certificates                                                                                              |
| `authorization-server.authentication-contexts`                 | `[USER, SYS]`                                       | Allowed contexts for this auth server                                                                                               |
| `authorization-server.claim-set-converter-name`                | —                                                   | Bean name of a custom claim set converter (`Converter<Map<String, Object>, Map<String, Object>>`) applied to the decoded JWT claims |
| `authorization-server.jwks-connect-timeout-in-millis`          | `15000`                                             | Connect timeout for fetching the JWKS                                                                                               |
| `authorization-server.jwks-read-timeout-in-millis`             | `15000`                                             | Read timeout for fetching the JWKS                                                                                                  |
| `authorization-server.introspection.uri`                       | `{issuer}/protocol/openid-connect/token/introspect` | Token introspection endpoint of this auth server                                                                                    |
| `authorization-server.introspection.client-id`                 | `resource-id`                                       | Confidential client used for introspection (identical to the resource id in the Keycloak setup)                                     |
| `authorization-server.introspection.client-secret`             | —                                                   | Secret of the introspection client (required when introspection is active)                                                          |
| `authorization-server.introspection.connect-timeout-in-millis` | `15000`                                             | Connect timeout for token introspection requests                                                                                    |
| `authorization-server.introspection.read-timeout-in-millis`    | `15000`                                             | Read timeout for token introspection requests                                                                                       |
| `authorization-server.introspection.mode`                      | —                                                   | Set to `NONE` to exclude this auth server from introspection (the only value accepted here)                                         |
| `auth-servers[*].*`                                            | —                                                   | List of additional auth servers (same fields as `authorization-server`)                                                             |
| `b2b-gateway.issuer`                                           | —                                                   | B2B gateway issuer (required)                                                                                                       |
| `b2b-gateway.jwk-set-uri`                                      | —                                                   | B2B gateway JWKS endpoint (required, not derived from the issuer)                                                                   |
| `b2b-gateway.authentication-contexts`                          | `[B2B]`                                             | Allowed contexts for the B2B gateway                                                                                                |
| `b2b-gateway.claim-set-converter-name`                         | —                                                   | Bean name of a custom claim set converter for B2B tokens                                                                            |
| `b2b-gateway.jwks-connect-timeout-in-millis`                   | `15000`                                             | Connect timeout for fetching the JWKS                                                                                               |
| `b2b-gateway.jwks-read-timeout-in-millis`                      | `15000`                                             | Read timeout for fetching the JWKS                                                                                                  |
| `b2b-gateway.introspection.*`                                  | —                                                   | Same fields as `authorization-server.introspection.*`                                                                               |
| `log-user-access`                                              | `false`                                             | Register a filter that logs each authenticated user's access                                                                        |
| `log.authentication-failure.enabled`                           | `false`                                             | Log failed authentications (invalid/missing bearer tokens)                                                                          |
| `log.access-denied.enabled`                                    | `false`                                             | Log authorization failures (authenticated but not permitted)                                                                        |
| `log.access-denied.debug`                                      | `false`                                             | Add debug detail to denied-request logs (needs `log.access-denied.enabled`)                                                         |

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
