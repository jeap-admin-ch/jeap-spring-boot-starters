# Swagger starter

`jeap-spring-boot-swagger-starter` is a convenience starter for serving the Swagger UI / OpenAPI
document from a jEAP service. It bundles [`jeap-spring-boot-swagger`](jeap-spring-boot-swagger.md) (the
core jEAP OpenAPI auto-configuration) together with `springdoc-openapi-starter-webmvc-ui` (the
springdoc Swagger UI and OpenAPI generation for Spring WebMVC).

Add this starter when you want the rendered Swagger UI. If you only need the OpenAPI configuration
(HTTPS enforcement, OAuth2 scheme, UI security) without the UI, depend on the bundled
[`jeap-spring-boot-swagger`](jeap-spring-boot-swagger.md) directly.

## What it bundles

| Dependency                            | Provides                                                                                          |
|---------------------------------------|---------------------------------------------------------------------------------------------------|
| `jeap-spring-boot-swagger`            | jEAP OpenAPI auto-config: access modes, OAuth2/OIDC scheme, HTTPS enforcement, actuator API group |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI and OpenAPI document generation for Spring WebMVC                                      |

## Usage

```xml
<dependency>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-spring-boot-swagger-starter</artifactId>
</dependency>
```

This starter has no properties of its own — all configuration comes from the bundled
[`jeap-spring-boot-swagger`](jeap-spring-boot-swagger.md) module under the `jeap.swagger.*` prefix.
The minimum to make the UI visible is to set `jeap.swagger.status` away from its `DISABLED` default:

```yaml
jeap:
  swagger:
    status: SECURED
    secured:
      username: swagger
      password: ${SWAGGER_PASSWORD}
```

With the default paths the UI is then served at `/swagger-ui.html` and the OpenAPI document at
`/api-docs`. See the [`jeap-spring-boot-swagger`](jeap-spring-boot-swagger.md) doc for the full
property reference, the access modes (`OPEN` / `SECURED` / `DISABLED` / `CUSTOM`), and the OAuth2/OIDC
login behaviour.

## Related

- [jeap-spring-boot-swagger](jeap-spring-boot-swagger.md)
- [Configuration property reference](configuration.md)
- [jeap-spring-boot-starters](../README.md)
