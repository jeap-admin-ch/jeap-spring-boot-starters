# Configuration property reference

This page consolidates the jEAP-specific configuration property prefixes contributed by the starters.
Each starter is only active when its dependency is on the classpath. For details and defaults, follow
the per-module page linked in each section. Standard Spring Boot / Spring Security / Togglz / Spring
Cloud Vault properties are not repeated here.

## Security — resource server (`jeap.security.oauth2.resourceserver.*`)

See [Security starter](jeap-spring-boot-security-starter.md).

| Property                                       | Default                                  | Description                                          |
|------------------------------------------------|------------------------------------------|------------------------------------------------------|
| `resource-id`                                  | —                                        | Expected token audience (restricted-audience tokens) |
| `application-name`                             | `${spring.application.name}`             | Default audience when `resource-id` is unset         |
| `system-name`                                  | —                                        | Activates semantic-role authorization when set       |
| `authorization-server.issuer`                  | —                                        | Single-auth-server issuer                            |
| `authorization-server.jwk-set-uri`             | `{issuer}/protocol/openid-connect/certs` | JWKS endpoint                                        |
| `authorization-server.authentication-contexts` | `[USER, SYS]`                            | Allowed authentication contexts                      |
| `auth-servers[*]`                              | —                                        | List of trusted auth servers (same fields)           |
| `b2b-gateway.issuer` / `.jwk-set-uri`          | —                                        | B2B gateway issuer / JWKS endpoint                   |
| `b2b-gateway.authentication-contexts`          | `[B2B]`                                  | Allowed contexts for the B2B gateway                 |
| `introspection.mode`                           | `NONE`                                   | `NONE`/`EXPLICIT`/`ALWAYS`/`LIGHTWEIGHT`/`CUSTOM`    |

Current-user endpoint: `jeap.security.oauth2.current-user-endpoint.enabled`,
`jeap.security.oauth2.current-user-endpoint.path` (`/api/current-user`).

OAuth2 client (`jeap-spring-boot-security-client-starter`) uses standard
`spring.security.oauth2.client.*` properties — see
[Security client starter](jeap-spring-boot-security-client-starter.md).

## Logging (`jeap.logging.*`, `jeap.rest.tracing.*`)

See [Logging starter](jeap-spring-boot-logging-starter.md) and
[REST request tracing](jeap-spring-boot-rest-request-tracing.md).

| Property                                                | Default                                                           | Description                           |
|---------------------------------------------------------|-------------------------------------------------------------------|---------------------------------------|
| `jeap.logging.rest.unhandled-exception-logging.enabled` | `false`                                                           | Log unhandled exceptions via a filter |
| `jeap.rest.tracing.header-masked`                       | `Authorization, Cookie, Set-Cookie, Set-Cookie2, x-jwt-assertion` | Headers masked in traces              |
| `jeap.rest.tracing.header-blacklist`                    | (empty)                                                           | Headers excluded from traces          |
| `jeap.rest.tracing.uri-filter-pattern`                  | `.*/actuator/.*`                                                  | URIs excluded from tracing            |

## Monitoring (`jeap.monitor.*`, `jeap.health.*`)

See [Monitoring starter](jeap-spring-boot-monitoring-starter.md).

| Property                                         | Default | Description                                |
|--------------------------------------------------|---------|--------------------------------------------|
| `jeap.monitor.prometheus.user` / `.password`     | —       | Prometheus endpoint Basic-auth credentials |
| `jeap.monitor.prometheus.secure`                 | `true`  | Secure the Prometheus endpoint             |
| `jeap.monitor.actuator.user` / `.password`       | —       | Admin actuator Basic-auth credentials      |
| `jeap.monitor.actuator.enable-admin-endpoints`   | `false` | Enable sensitive/admin actuator endpoints  |
| `jeap.health.metric.update-rate-seconds`         | `120`   | Health metric refresh interval             |
| `jeap.health.metric.contributor-metrics.enabled` | `false` | Per-component health metrics               |

## Secrets & data (`jeap.vault.*`, `jeap.datasource.*`, `jeap.s3.client.*`)

See [Vault starter](jeap-spring-boot-vault-starter.md),
[PostgreSQL AWS starter](jeap-spring-boot-postgresql-aws-starter.md),
[Object storage starter](jeap-spring-boot-object-storage-starter.md) and
[Transaction routing](jeap-spring-boot-tx.md).

| Property                                                    | Default                   | Description                           |
|-------------------------------------------------------------|---------------------------|---------------------------------------|
| `jeap.vault.url`                                            | —                         | Vault server URL                      |
| `jeap.vault.system-name`                                    | —                         | System identifier for the secret path |
| `jeap.vault.app-role.role-id` / `.secret-id`                | —                         | Vault AppRole credentials             |
| `jeap.datasource.aws.region`                                | `eu-central-2`            | AWS region for RDS                    |
| `jeap.datasource.aws.hostname` / `.port` / `.database-name` | — / `5432` / —            | RDS endpoint, port, database          |
| `jeap.datasource.replica.enabled`                           | `false`                   | Enable a read-replica datasource      |
| `jeap.s3.client.enabled`                                    | `true`                    | Create the `S3Client` bean            |
| `jeap.s3.client.endpoint-url` / `.region` / `.tls`          | — / `AWS_GLOBAL` / `true` | S3 endpoint, region, HTTPS            |

## Web, Swagger & feature flags (`jeap.web.headers.*`, `jeap.swagger.*`, `togglz.*`)

See [Web config starter](jeap-spring-boot-web-config-starter.md),
[Swagger](jeap-spring-boot-swagger.md) /
[Swagger starter](jeap-spring-boot-swagger-starter.md) and
[Feature flag starter](jeap-spring-boot-featureflag-starter.md).

| Property                                      | Default             | Description                            |
|-----------------------------------------------|---------------------|----------------------------------------|
| `jeap.web.headers.additional-content-sources` | —                   | Extra CSP-allowed URLs                 |
| `jeap.web.headers.skip-path-prefixes`         | `/api`, `/actuator` | Paths without security/caching headers |
| `jeap.swagger.status`                         | `DISABLED`          | Swagger access mode                    |
| `jeap.swagger.enforce-server-base-https`      | `true`              | Force HTTPS in server URLs             |
| `togglz.features.*`                           | —                   | Togglz feature-flag definitions        |

## Related

- [Getting started](getting-started.md)
- [Security starter](jeap-spring-boot-security-starter.md)
- [Monitoring starter](jeap-spring-boot-monitoring-starter.md)
- [jeap-spring-boot-starters](../README.md)
