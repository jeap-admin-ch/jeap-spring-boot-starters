# Getting started

This page shows how to add a jEAP Spring Boot starter to a service. Each starter is an
auto-configuration module: adding the dependency activates jEAP-aligned defaults, and you customise
behaviour through `jeap.*` configuration properties. The version is managed by the jEAP Spring Boot
parent, so you never declare a version on these dependencies.

## 1. Pick the starters you need

A typical jEAP backend service combines a few starters. The most common combination is:

```xml
<dependency>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-spring-boot-application-starter</artifactId>
</dependency>
<dependency>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-spring-boot-monitoring-starter</artifactId>
</dependency>
<dependency>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-spring-boot-security-starter</artifactId>
</dependency>
```

`jeap-spring-boot-application-starter` already pulls in `jeap-spring-boot-logging-starter`, so you do
not add the logging starter separately. See the [Modules table](../README.md) for the full list.

## 2. Secure the REST API

The [security starter](jeap-spring-boot-security-starter.md) turns the service into an OAuth2 resource
server. Point it at the authorization server (Keycloak) and validation of the JWT happens automatically:

```yaml
jeap:
  security:
    oauth2:
      resourceserver:
        system-name: mysystem
        authorization-server:
          issuer: https://keycloak.example.ch/auth/realms/myrealm
```

Set `system-name` to your service's name — setting it activates semantic-role authorization
(structured `system_%tenant_@resource_#operation` roles instead of opaque strings). See the
[security starter docs](jeap-spring-boot-security-starter.md#authorization) for details.

Then guard endpoints with role checks — see the
[security starter authorization section](jeap-spring-boot-security-starter.md#authorization).

## 3. Expose monitoring

The [monitoring starter](jeap-spring-boot-monitoring-starter.md) adds Actuator, Micrometer and a
Prometheus endpoint at
`/actuator/prometheus`, secured with Basic auth by default:

```yaml
jeap:
  monitor:
    prometheus:
      user: prometheus
      password: <encrypted>
```

## 4. Configure logging

`jeap-spring-boot-logging-starter` is active as soon as it is on the classpath. It produces
human-readable console output locally and switches to
[structured JSON](jeap-spring-boot-logging-starter.md) when the `cloudwatch` or `rhos` Spring profile
is active, enriching every line with `traceId` and `spanId`.

## 5. Add the other concerns as needed

| Need                              | Starter / page                                                       |
|-----------------------------------|----------------------------------------------------------------------|
| Call another secured service      | [OAuth2 client](jeap-spring-boot-security-client-starter.md)         |
| HashiCorp Vault secrets           | [Vault starter](jeap-spring-boot-vault-starter.md)                   |
| AWS RDS PostgreSQL                | [PostgreSQL AWS starter](jeap-spring-boot-postgresql-aws-starter.md) |
| S3 object storage                 | [Object storage starter](jeap-spring-boot-object-storage-starter.md) |
| Swagger UI / OpenAPI              | [Swagger starter](jeap-spring-boot-swagger-starter.md)               |
| Feature flags                     | [Feature flag starter](jeap-spring-boot-featureflag-starter.md)      |
| Frontend security/caching headers | [Web config starter](jeap-spring-boot-web-config-starter.md)         |

## Related

- [Security starter](jeap-spring-boot-security-starter.md)
- [Monitoring starter](jeap-spring-boot-monitoring-starter.md)
- [Logging starter](jeap-spring-boot-logging-starter.md)
- [Configuration property reference](configuration.md)
- [jeap-spring-boot-starters](../README.md)
