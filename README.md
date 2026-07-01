# jEAP Spring Boot Starters

jEAP Spring Boot Starters is a collection of Spring Boot starters used when developing a Spring Boot
application based on jEAP. Each starter auto-configures a single cross-cutting concern with
jEAP-aligned defaults, so a service can focus on business logic instead of infrastructure wiring. The
starters cover:

* OAuth2 resource-server security with JWT validation and simple or semantic role-based authorization
* OAuth2 client support for calling other secured services (client credentials and token forwarding)
* Structured JSON logging with distributed-tracing context (`traceId`/`spanId`)
* Monitoring via Spring Boot Actuator, Micrometer and a secured Prometheus endpoint
* Secrets management with HashiCorp Vault, AWS RDS PostgreSQL with IAM auth, S3 object storage
* Feature flags (Togglz), OpenAPI/Swagger UI, frontend route handling and web security headers

The Maven parent is `ch.admin.bit.jeap:jeap-internal-spring-boot-parent`, which pins the Spring Boot
and dependency versions; this repository does not declare them directly.

## Documentation

Start with [Getting started](docs/getting-started.md). The two general pages below apply across the
starters; for everything else there is one page per module (see the [Modules](#modules) table, where
each documented module links to its own `docs/<module-name>.md`).

| General documentation                                         | File                                                |
|---------------------------------------------------------------|-----------------------------------------------------|
| Getting started (add a starter, minimal config)               | [docs/getting-started.md](docs/getting-started.md)  |
| Configuration property reference                              | [docs/configuration.md](docs/configuration.md)      |

Per-module documentation lives in `docs/<module-name>.md` — each module in the table below links to
its own page.

## Modules

Group id for all modules is `ch.admin.bit.jeap`; the version is managed by the jEAP Spring Boot
parent. Modules ending in `-it`, `-it-*` or `-test` are test-support or integration-test modules.

| Module                                          | Purpose                                                                                  |
|-------------------------------------------------|------------------------------------------------------------------------------------------|
| [`jeap-spring-boot-application-starter`](docs/jeap-spring-boot-application-starter.md) | Frontend route handling, HikariCP pooling and reverse-proxy defaults; bundles `logging-starter` |
| [`jeap-spring-boot-logging-starter`](docs/jeap-spring-boot-logging-starter.md) | Structured JSON logging with tracing context, profile-based appenders (CloudWatch/RHOS/console) |
| [`jeap-spring-boot-monitoring-starter`](docs/jeap-spring-boot-monitoring-starter.md) | Actuator + Micrometer/Prometheus metrics with a secured Prometheus endpoint              |
| [`jeap-spring-boot-security-starter`](docs/jeap-spring-boot-security-starter.md) | OAuth2 resource server: JWT validation, simple & semantic role authorization             |
| [`jeap-spring-boot-security-client-starter`](docs/jeap-spring-boot-security-client-starter.md) | OAuth2 client: preconfigured `RestClient` builders (client credentials / token forwarding) |
| [`jeap-spring-boot-security-starter-test`](docs/jeap-spring-boot-security-starter-test.md) | Test support: token builders, `@WithJeapAuthenticationToken`, mock auth servers          |
| [`jeap-spring-boot-rest-request-tracing`](docs/jeap-spring-boot-rest-request-tracing.md) | Servlet REST request/response tracing with header masking                                |
| [`jeap-spring-boot-vault-starter`](docs/jeap-spring-boot-vault-starter.md) | Secrets management via HashiCorp Vault (Spring Cloud Vault, AppRole/Kubernetes auth)     |
| [`jeap-spring-boot-object-storage-starter`](docs/jeap-spring-boot-object-storage-starter.md) | S3-compatible object-storage client (`S3Client`) configuration                           |
| [`jeap-spring-boot-postgresql-aws-starter`](docs/jeap-spring-boot-postgresql-aws-starter.md) | AWS RDS PostgreSQL with IAM auth and optional read replicas (AWS Advanced JDBC Wrapper)  |
| [`jeap-spring-boot-tx`](docs/jeap-spring-boot-tx.md) | Read-replica-aware transaction routing (`@TransactionalReadReplica`)                     |
| [`jeap-spring-boot-web-config-starter`](docs/jeap-spring-boot-web-config-starter.md) | Frontend security headers (CSP, HSTS, ...) and caching headers                           |
| [`jeap-spring-boot-swagger`](docs/jeap-spring-boot-swagger.md) | OpenAPI/Swagger configuration (springdoc) with OAuth2 and HTTPS enforcement              |
| [`jeap-spring-boot-swagger-starter`](docs/jeap-spring-boot-swagger-starter.md) | Convenience starter bundling `jeap-spring-boot-swagger` and the springdoc Swagger UI     |
| [`jeap-spring-boot-featureflag-starter`](docs/jeap-spring-boot-featureflag-starter.md) | Feature flags via Togglz with Spring Cloud config refresh                                |

## Changes

This library is versioned using [Semantic Versioning](http://semver.org/) and all changes are documented in
[CHANGELOG.md](./CHANGELOG.md) following the format defined in [Keep a Changelog](http://keepachangelog.com/).

## Note

This repository is part the open source distribution of jEAP. See [github.com/jeap-admin-ch/jeap](https://github.com/jeap-admin-ch/jeap)
for more information.

## Attributions

This project includes code from the following open-source projects:

1. **[Spring Framework]**  
   Link: [https://github.com/spring-projects](https://github.com/spring-projects)  
   License: Apache 2.0
   Included Code: jEAP is based on the Spring Framework and Spring Boot. Small code snippets from the Spring Framework
   are included in this project, namely in the jeap-spring-boot-config-starter.            
   Changes: Minor modifications to fit project requirements.

## License

This repository is Open Source Software licensed under the [Apache License 2.0](./LICENSE).
