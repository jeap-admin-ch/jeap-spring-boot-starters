# Swagger / OpenAPI

`jeap-spring-boot-swagger` provides the core jEAP OpenAPI/Swagger auto-configuration on top of
[springdoc-openapi](https://springdoc.org/). OpenAPI (formerly the Swagger Specification) is the
standard used in jEAP services to describe REST APIs; springdoc generates the OpenAPI document from
Spring REST controllers and OpenAPI annotations, and the Swagger UI renders it interactively.

This module contains **only the auto-configuration** — it does not bring the Swagger UI itself. To
also serve the UI, use [`jeap-spring-boot-swagger-starter`](jeap-spring-boot-swagger-starter.md), which
bundles this module plus the springdoc Swagger UI. Use this module directly when you only need the
OpenAPI configuration (HTTPS enforcement, OAuth2 scheme, UI security) without the rendered UI.

## What it configures

| Class                             | Purpose                                                                                                               |
|-----------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| `SwaggerProperties`               | `@ConfigurationProperties("jeap.swagger")`; loads `SwaggerDefaultConfiguration.properties`                            |
| `SwaggerWebSecurityConfig`        | A high-precedence `SecurityFilterChain` securing the Swagger paths, driven by `jeap.swagger.status`                   |
| `SwaggerOauthConfiguration`       | Declares the `OIDC` OpenID-Connect security scheme for the UI (only when a resource-server issuer is set)             |
| `HttpsServerBaseUrlConfiguration` | Rewrites `http://` server URLs to `https://` (except `localhost`) when `enforce-server-base-https`                    |
| `ActuatorSwaggerConfig`           | Adds an `Actuator` API group (when `springdoc.show-actuator` and the monitoring `ActuatorSecurity` class are present) |

### Access modes (`jeap.swagger.status`)

`SwaggerWebSecurityConfig` registers a dedicated filter chain at order
`HIGHEST_PRECEDENCE + 8` that only matches the Swagger paths (`antPathPatters`, see below), so it
overrides the application's own security for those paths. Behaviour by status:

- **`OPEN`** — `permitAll()`; the UI is reachable without authentication (handy for local profiles).
- **`SECURED`** — HTTP Basic auth requiring the `swagger` role; credentials come from an in-memory
  user built from `jeap.swagger.secured.username` / `password`. Startup fails if either is missing.
- **`DISABLED`** — `denyAll()` for the Swagger paths (the default).
- **`CUSTOM`** — the filter chain is not registered at all (`@ConditionalOnExpression` excludes
  `CUSTOM`), leaving the application fully responsible for securing the UI.

### OAuth2 / OIDC login in the UI

When `jeap.security.oauth2.resourceserver.authorization-server.issuer` is set,
`SwaggerOauthConfiguration` declares an `OIDC` security scheme (`SecuritySchemeType.OPENIDCONNECT`)
with both `authorizationCode` and `clientCredentials` flows, so the Swagger UI offers both an end-user
(authorization-code) and a system (client-credentials) login. The OIDC discovery URL defaults to the
issuer's `/.well-known/openid-configuration`. The UI uses PKCE with the authorization-code grant
(`springdoc.swagger-ui.oauth.use-pkce-with-authorization-code-grant=true`).

### HTTPS enforcement

jEAP services typically listen on HTTP behind a TLS-terminating reverse proxy, so the server URL
springdoc derives from the request would be `http://...`. `HttpsServerBaseUrlConfiguration` registers a
`ServerBaseUrlCustomizer` that rewrites the scheme to `https` for non-`localhost` hosts, so the
"Try it out" calls in the UI target the externally reachable HTTPS endpoint. Disable with
`jeap.swagger.enforce-server-base-https=false`.

### Actuator API group

If springdoc actuator exposure is on and the jEAP monitoring `ActuatorSecurity` class is on the
classpath, `ActuatorSwaggerConfig` adds a `GroupedOpenApi` named `Actuator` matching the actuator base
path, titled "Monitoring Endpoints", protected by a `prometheus` HTTP-basic security scheme.

## Properties

Prefix: `jeap.swagger` (defaults from `SwaggerDefaultConfiguration.properties`).

| Property                                 | Default                                                                        | Description                                                 |
|------------------------------------------|--------------------------------------------------------------------------------|-------------------------------------------------------------|
| `jeap.swagger.status`                    | `DISABLED`                                                                     | `OPEN`, `SECURED`, `DISABLED` or `CUSTOM` (see above)       |
| `jeap.swagger.ant-path-patters`          | `/swagger-ui.html`, `/api-docs/**`, `/swagger-ui/**`, `/webjars/swagger-ui/**` | Paths the security filter chain matches                     |
| `jeap.swagger.enforce-server-base-https` | `true`                                                                         | Rewrite `http` to `https` in server URLs (except localhost) |
| `jeap.swagger.oauth.open-id-connect-url` | `${issuer}/.well-known/openid-configuration`                                   | OIDC discovery endpoint for the UI OAuth2 login             |
| `jeap.swagger.secured.username`          | `swagger`                                                                      | Basic-auth user when `status=SECURED`                       |
| `jeap.swagger.secured.password`          | —                                                                              | Basic-auth password when `status=SECURED` (required)        |

> Note: the property is genuinely spelled `ant-path-patters` (missing `n`) in the source — bind it
> with that spelling.

Relevant springdoc defaults set by this module: `springdoc.swagger-ui.path=/swagger-ui.html`,
`springdoc.api-docs.path=/api-docs`, `springdoc.swagger-ui.tryItOutEnabled=true`,
`springdoc.show-actuator=true`.

## Patterns / pitfalls

- The Swagger filter chain takes precedence over the application's security for the Swagger paths —
  switch to `CUSTOM` if you need to integrate Swagger access into your own security configuration.
- `SECURED` without `jeap.swagger.secured.password` fails at startup by design.
- Document your API with springdoc annotations (`@OpenAPIDefinition`, `@Tag`, `@Operation`); jEAP
  convention is to set `info.title`/`description`/`contact`/`version` and an `externalDocs` link, and
  to use the `OIDC` `@SecurityRequirement` on OAuth2-protected operations.

## Related

- [jeap-spring-boot-swagger-starter](jeap-spring-boot-swagger-starter.md)
- [Configuration property reference](configuration.md)
- [jeap-spring-boot-starters](../README.md)
