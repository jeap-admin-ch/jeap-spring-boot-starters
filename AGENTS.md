# AGENTS.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

jEAP Spring Boot Starters is a collection of ~21 Spring Boot auto-configuration modules providing standardized cross-cutting concerns (logging, monitoring, security, OAuth2, Vault, S3, feature flags, etc.) for jEAP-based applications. Maintained by the Federal Office of Information Technology, Systems and Telecommunication (FOITT). Released as open source (Apache 2.0) as part of the jEAP platform.

The Maven parent is `ch.admin.bit.jeap:jeap-internal-spring-boot-parent`, which pins Spring Boot and dependency versions; this repo does not declare them directly.

## Build Commands

```bash
# Full build with tests
./mvnw clean install

# Build without tests
./mvnw install -Dmaven.test.skip=true

# Run tests for a single module
./mvnw test -pl jeap-spring-boot-security-starter

# Run a specific test class
./mvnw test -pl jeap-spring-boot-security-starter-it-webmvc -Dtest=SimpleRoleAuthorizationWebmvcIT

# Run a specific test method
./mvnw test -pl jeap-spring-boot-security-starter-it-webmvc -Dtest=SimpleRoleAuthorizationWebmvcIT#testGetAuth_whenWithUserRoleAuthRead_thenAccessGranted
```

## Architecture

### Module Organization

The project uses a multi-module Maven structure with these categories:

1. **Starter modules** (production code): `jeap-spring-boot-*-starter`
   - Auto-configure Spring Boot applications via `@AutoConfiguration`
   - Register via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

2. **Core/support modules** (no `-starter` suffix): shared building blocks consumed by starters
   - `jeap-spring-boot-swagger` (OpenAPI core, wrapped by `swagger-starter`), `jeap-spring-boot-tx` (transaction support), `jeap-spring-boot-rest-request-tracing`
   - `jeap-spring-boot-security-starter-test`: test-support library providing helpers/mocks for testing secured apps

3. **Integration test base modules** (`*-it`): contain abstract test classes (`Abstract*IT.java`) with shared test logic

4. **Stack-specific integration test modules**: extend the abstract `-it` tests for a concrete web stack
   - `*-it-webmvc` / `*-it-mvc` use Spring MVC + `MockMvc`
   - `*-it-servlet` targets the plain servlet stack
   - (No WebFlux integration test modules currently exist.)

### Starters

| Module                    | Purpose                                                                  | Config Prefix                           |
|---------------------------|--------------------------------------------------------------------------|-----------------------------------------|
| `application-starter`     | Frontend route handling + DB pooling defaults; bundles `logging-starter` | —                                       |
| `logging-starter`         | Structured JSON logging with tracing                                     | `jeap.logging.*`                        |
| `monitoring-starter`      | Prometheus/Micrometer metrics                                            | `jeap.monitor.*`                        |
| `security-starter`        | OAuth2 resource server, role authorization                               | `jeap.security.oauth2.resourceserver.*` |
| `security-client-starter` | OAuth2 client configuration                                              | `jeap.security.oauth2.client.*`         |
| `featureflag-starter`     | Feature flags via config properties + Togglz                             | —                                       |
| `object-storage-starter`  | S3 client configuration                                                  | —                                       |
| `postgresql-aws-starter`  | PostgreSQL config for AWS RDS (multiple cluster types)                   | —                                       |
| `vault-starter`           | HashiCorp Vault secrets management                                       | Spring Cloud Vault config               |
| `web-config-starter`      | HTTP caching + frontend security headers                                 | —                                       |
| `swagger-starter`         | OpenAPI specs + Swagger UI                                               | `jeap.swagger.*`                        |

## Code Patterns

### Auto-Configuration Pattern

```java
@AutoConfiguration
@Conditional(SomeCondition.class)
@ConfigurationProperties("jeap.module.config")
@Validated
@Data  // Lombok
public class ModuleProperties {
    @NestedConfigurationProperty
    @Valid
    private NestedConfig nested;
}
```

### Package Structure

All code under `ch.admin.bit.jeap.[domain].*`:
- `*.configuration` - Auto-configuration classes
- `*.properties` - Configuration properties classes
- `*.condition` - Custom Spring Condition implementations

### Testing Patterns

- Integration tests end with `*IT.java`; unit tests end with `*Test.java`
- Abstract integration tests live in `*-it` modules; concrete tests in `*-it-webmvc`/`*-it-servlet` extend them
- WireMock is used for mocking external services (e.g. OAuth2 providers)

### Dependencies

- Prefer Java records over Lombok
- **Spring Boot 4.x**: uses the `@AutoConfiguration` pattern (not legacy `spring.factories`)
- **Java 25**: required minimum version

## Versioning & Conventions

- Semantic Versioning; all changes documented in [CHANGELOG.md](./CHANGELOG.md) (Keep a Changelog format).
- `setPomVersions.sh` updates the version across all module POMs.
- When working on a feature branch, increase the version to `x.y.z-SNAPSHOT` in the POMs.
- When bumping the version, also  update the changelog, and updates version/date in `publiccode.yml`.
- When the version on a feature branch has not yet been bumped compared to master, ask the user if a major, minor or patch version bump should be performed, and update the version accordingly.
