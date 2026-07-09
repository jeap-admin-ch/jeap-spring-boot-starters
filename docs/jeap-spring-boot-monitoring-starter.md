# Monitoring starter

`jeap-spring-boot-monitoring-starter` configures technical monitoring for jEAP services: Spring Boot
Actuator endpoints, Micrometer metrics, a Prometheus scrape endpoint, OpenTelemetry tracing, and the
security around the actuator endpoints. Technical monitoring observes the availability, performance
and overall state of an executable component, so that deviations from "normal" values can be detected
early and unexpected behaviour understood. It is distinct from [logging](jeap-spring-boot-logging-starter.md)
and from business (functional) monitoring.

Each microservice exposes its own technical metrics (memory, CPU, threads, ...) and a small set of
HTTP endpoints through which an external system (Prometheus via Promregator, Grafana, Spring Boot
Admin) can observe it. The starter is the recommended, consistent way to wire those endpoints and
their security in line with the jEAP requirements.

## Adding it

```xml
<dependency>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-spring-boot-monitoring-starter</artifactId>
</dependency>
```

At minimum, set a real Prometheus password in your application (the shipped default is a placeholder
hash that matches no value). The password should come from Vault or a user-provided service and may be
stored encrypted, e.g. with bcrypt.

## Requirements it implements

- Every backend application must expose a Prometheus endpoint (pure UI apps are exempt).
- The Prometheus endpoint must not be publicly accessible — it is secured with Basic auth by default,
  because it leaks information an attacker could exploit.
- Each application should expose a health and an info endpoint (info should carry at least the
  application name and version).
- Actuators that can expose sensitive data must be off on acceptance/production, and write access must
  be blocked there. Using this starter is the recommended way to achieve that consistently.

## Endpoints

Endpoints are disabled by default and selectively enabled (`management.endpoints.enabled-by-default=false`,
JMX exposure excluded, web exposure `*`). Security is wired by the `ActuatorSecurity` filter chain at
high precedence (`HIGHEST_PRECEDENCE + 9`), with two in-memory Basic-auth users.

| Endpoint                                                                                    | Path                   | Default access                                                                                               |
|---------------------------------------------------------------------------------------------|------------------------|--------------------------------------------------------------------------------------------------------------|
| Index                                                                                       | `/actuator`            | Role `ACTUATOR` (the overview page)                                                                          |
| Health                                                                                      | `/actuator/health`     | Public; details/components shown only `when-authorized` (role `ACTUATOR`); liveness/readiness probes enabled |
| Info                                                                                        | `/actuator/info`       | Public (exposes `info.*`, including build name/version)                                                      |
| Prometheus                                                                                  | `/actuator/prometheus` | Secured with Basic auth (role `PROMETHEUS`) unless `jeap.monitor.prometheus.secure=false`                    |
| Admin (`beans`, `configprops`, `env`, `loggers`, `metrics`, `scheduledtasks`, `threaddump`) | `/actuator/*`          | Disabled unless `jeap.monitor.actuator.enable-admin-endpoints=true`; then GET-only, role `ACTUATOR`          |

Notable details of the security chain:

- Admin endpoints are matched **GET-only** by default. When admin endpoints are enabled, the
  `loggers` endpoint additionally matches POST so log levels can be changed from Spring Boot Admin —
  every other endpoint stays read-only, satisfying the "no write access on PRD/ABN" requirement.
- Any actuator request not explicitly permitted is denied (`anyRequest().denyAll()`), CSRF is disabled
  for the actuator chain, and the password encoder is a delegating encoder that does not auto-upgrade.

## Configuration

Prefix: `jeap.monitor`. Default users are `prometheus` and `actuator`.

| Property                                               | Default          | Description                                                                                            |
|--------------------------------------------------------|------------------|--------------------------------------------------------------------------------------------------------|
| `jeap.monitor.prometheus.user`                         | `prometheus`     | Username for the Prometheus endpoint.                                                                  |
| `jeap.monitor.prometheus.password`                     | placeholder hash | Password for the Prometheus endpoint; may be encrypted. **Override this.**                             |
| `jeap.monitor.prometheus.secure`                       | `true`           | Secure the Prometheus endpoint with Basic auth; `false` makes it public.                               |
| `jeap.monitor.actuator.user`                           | `actuator`       | Username for the admin/health-detail user (role `ACTUATOR`).                                           |
| `jeap.monitor.actuator.password`                       | placeholder hash | Password for the `ACTUATOR` user; may be encrypted.                                                    |
| `jeap.monitor.actuator.enable-admin-endpoints`         | `false`          | Enable the Spring Boot Admin / sensitive actuator endpoints. Not for production.                       |
| `jeap.monitor.actuator.permitted-endpoints`            | preset list      | Actuator endpoint classes accessible to the `ACTUATOR` role (do not override; use the property below). |
| `jeap.monitor.actuator.additional-permitted-endpoints` | (empty)          | Additional application-provided endpoint classes for the `ACTUATOR` role.                              |

## Metrics

Beyond the standard JVM/system metrics exposed via Micrometer, the starter registers jEAP-specific
metrics, all surfaced on the Prometheus endpoint:

| Metric                                  | Type        | Tags                                                               | Description                                                                                           |
|-----------------------------------------|-------------|--------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| `health`                                | Gauge       | —                                                                  | Overall health (1 = UP, 0 = otherwise), refreshed every `update-rate-seconds`.                        |
| `health_indicator_status`               | Gauge       | `component`                                                        | Per-contributor health (only when `contributor-metrics.enabled=true`).                                |
| `jeap_spring_app`                       | Multi-gauge | `name`                                                             | The Spring application name (value always 1) — correlates platform app name with the Spring app name. |
| `jeap_dependency_version`               | Multi-gauge | `name`, `version`                                                  | One row per detected dependency (value 1) for central version checks.                                 |
| `jeap_java_version`                     | Multi-gauge | `version`, `vmversion`, `vendor`, `runtimeversion`, `classversion` | The running Java version.                                                                             |
| `jeap_relation` (`jeap_relation_total`) | Counter     | `producer`, `consumer`, `datapoint`, `technology`, `method`        | Incoming REST relations between services (producer = this app, consumer = caller, datapoint = URI).   |
| `jeap_rest_endpoint_without_jwt`        | Counter     | `producer`, `datapoint`, `method`, `status`, `auth`                | REST endpoints reached without a jEAP JWT (helps find unauthenticated access).                        |
| `jeap_trusted_cert`                     | Multi-gauge | `subject`, `serial`, `from`, `to`                                  | Days until each trust-store certificate expires (0 if unknown).                                       |

The two REST counters are cardinality-capped to protect Prometheus from a label explosion: once the
limit is reached, new label combinations are dropped.

| Property                                                            | Default | Description                                                                       |
|---------------------------------------------------------------------|---------|-----------------------------------------------------------------------------------|
| `jeap.health.metric.update-rate-seconds`                            | `120`   | Health-metric refresh interval, in seconds. `-1` disables the metric.             |
| `jeap.health.metric.contributor-metrics.enabled`                    | `false` | Export per-contributor health as `health_indicator_status`.                       |
| `jeap.monitor.metrics.truststore.enabled`                           | `true`  | Export `jeap_trusted_cert` metrics (only when both `javax.net.ssl.trustStore` and `javax.net.ssl.trustStorePassword` are set). |
| `jeap.monitor.metrics.rest.maximum-allowable-jeap-relation-metrics` | `2000`  | Cardinality cap for `jeap_relation`.                                              |
| `jeap.monitor.metrics.security.maximum-allowable-metrics`           | `1000`  | Cardinality cap for `jeap_rest_endpoint_without_jwt`.                             |

Custom application metrics are added the usual way, by injecting Micrometer's `MeterRegistry`.

## Tracing

OpenTelemetry tracing is configured to emit both W3C and B3 on outbound calls and to accept W3C, B3
and B3-multi on inbound calls — keeping cross-service correlation working while the fleet migrates
from B3 (Brave) to W3C. Metric export over OTLP is disabled (metrics go through the Prometheus
endpoint), and OTLP **trace** export is off by default: it activates only when an application sets
`management.opentelemetry.tracing.export.otlp.endpoint`. Until then the tracing stack still runs
locally, so `traceId`/`spanId` are available in the MDC and logs.

## Common pitfalls

- Leaving the placeholder Prometheus/actuator passwords in place makes Basic auth effectively unusable
  — always override them per application.
- Setting `jeap.monitor.prometheus.secure=false` makes `/actuator/prometheus` public; only do this in
  trusted/local setups, never on ABN/PRD.
- Do not override `permitted-endpoints` (it carries the safe default set); add your own endpoints via
  `additional-permitted-endpoints` instead.
- `enable-admin-endpoints=true` is intended for development/Spring Boot Admin and must stay `false` on
  production and acceptance.

## Related

- [jeap-spring-boot-logging-starter](jeap-spring-boot-logging-starter.md)
- [Configuration property reference](configuration.md)
- [jeap-spring-boot-starters](../README.md)
