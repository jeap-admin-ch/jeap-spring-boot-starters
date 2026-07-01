# Logging starter

`jeap-spring-boot-logging-starter` standardises application logging across jEAP services. It is active
as soon as it is on the classpath (and is bundled by
[`jeap-spring-boot-application-starter`](jeap-spring-boot-application-starter.md)), so most services
inherit it transitively and never depend on it directly.

Logging records the run of a software process for the purpose of tracing and reproducing error
states. In a microservice landscape every service writes its own log, but a single business process
typically touches several systems, so per-file analysis quickly becomes impractical. The starter
therefore produces a uniform, structured, trace-correlated log that can be shipped to a central
backend (Splunk on the BIT platform) and searched across services. Logging is deliberately distinct
from [monitoring](jeap-spring-boot-monitoring-starter.md) (the technical health of a service) and from
business-event logging (the recording of domain events), which use their own dedicated appenders.

## Adding it

The starter is normally pulled in transitively by the application starter. If you need it on its own:

```xml
<dependency>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-spring-boot-logging-starter</artifactId>
</dependency>
```

No further configuration is required for local development: with no platform set the starter logs
human-readable text to stdout.

## Principles (jEAP requirements)

- **Log against the SLF4J facade** and instantiate one logger per class, so the originating class is
  always visible and the log implementation stays exchangeable.
- **Structure log entries as JSON** with the standard fields, so they can be processed uniformly and
  share a common vocabulary across services.
- **Use `StructuredArguments`** (`net.logstash.logback.argument.StructuredArguments`) instead of
  string concatenation, so argument values become first-class JSON fields.
- **Every request carries a `JEAP-APPLICATION-NAME` header**, generated automatically by the starter,
  so the receiver can tell which application made a call.
- **Never log security-relevant data** (credentials, tokens in HTTP headers), especially on
  production. Spring Boot's `defaults.xml` is included; known-noisy lines (e.g. the Kafka
  "Found no committed offset for partition" message) are suppressed via a `LogMessageFilter`
  turbo-filter, and the verbose Kafka `*Config` loggers are pinned to `ERROR`.

## Output formats and appenders

The starter ships `logback-spring.xml`, which selects exactly one main appender (`MAIN_APPENDER`) at
runtime. The choice is **not** driven by Spring profiles but by `JeapLogConfigurationContextListener`,
a Logback context listener that reads context properties bound from Spring (`jeap.logging.platform`,
`spring.application.name`, `spring.boot.admin.client.*`) plus the `rollingLogFile` Spring profile.

| Format        | Selected when                      | Output                                                                                          |
|---------------|------------------------------------|-------------------------------------------------------------------------------------------------|
| `consoletext` | default (no platform set)          | Human-readable console text, pattern `%d %-5level [${app},traceId,spanId] %logger{35} - %msg%n` |
| `cloudwatch`  | `jeap.logging.platform=cloudwatch` | Structured JSON for AWS CloudWatch, incl. ECS container metadata                                |
| `rhos`        | `jeap.logging.platform=rhos`       | Structured JSON for RHOS / OpenShift                                                            |

In addition, a secondary `ROLLING_FILE` appender is added (alongside the main appender) when the
`rollingLogFile` Spring profile is active, or when a Spring Boot Admin client is configured
(`spring.boot.admin.client.enabled=true`, or a non-blank `spring.boot.admin.client.url` that is not
explicitly disabled). It is a `RollingFileAppender` writing `log.log` with a fixed-window policy
(1 backup, `log.log.1`) and a 5 MB size trigger — this feeds the Spring Boot Admin log view. The root
logger is set to `INFO` in all cases.

### Structured JSON fields

The `cloudwatch` and `rhos` encoders use logstash's `LoggingEventCompositeJsonEncoder` and emit:
`timestamp`, `app` (from `spring.application.name`), `logger` (shortened to 20 chars), `level`,
`tags`, `threadName`, `mdc` (includes `traceId`/`spanId`), `arguments` (the `StructuredArguments`),
logstash markers, `message`, `exception` (a shortened stack trace — max depth 40, 4096 chars, root
cause first) and `exception-hash` (a stable hash for grouping identical stack traces). The
`cloudwatch` format additionally exposes `taskDefinitionVersion`, read once at startup from the ECS
container metadata endpoint (`ECS_CONTAINER_METADATA_URI_V4`).

## Distributed tracing

Every log line is enriched with the current `traceId` and `spanId` from the MDC. The starter brings
the `micrometer-tracing-bridge-otel` bridge, so the trace context populated by Spring Boot's
observation / Micrometer Tracing instrumentation flows into the MDC and appears both in the console
pattern (`[app,traceId,spanId]`) and in the JSON `mdc` field. This lets a single trace be followed
across services in the central backend. The jEAP requirement is to use the trace id for cross-system
error analysis and (recommended) to export traces via OpenTelemetry — OTLP trace export itself is
wired by the [monitoring starter](jeap-spring-boot-monitoring-starter.md).

## REST request logging

`RestRequestLogger` (registered in servlet web applications) listens on the
[`jeap-spring-boot-rest-request-tracing`](jeap-spring-boot-rest-request-tracing.md) tracer and logs
REST calls with structured arguments under the logger topic
`ch.admin.bit.jeap.log.RestRequestTracer` — so it is controlled by that logger's level:

- Incoming requests are logged at `TRACE` (`method`, `uri`).
- Responses are logged at `DEBUG` (`method`, `uri`, `result` status, `caller`, `user`, `dt` elapsed
  ms, `remoteAddr`, and — when full detail is enabled — `requestHeaders`, `responseHeaders`,
  `attributes`).

URI filtering and header masking are governed by the tracer's `jeap.rest.tracing.*` configuration in
the rest-request-tracing module.

### Unhandled exception logging

Enabling `jeap.logging.rest.unhandled-exception-logging.enabled=true` registers a servlet filter
(`UnhandledExceptionLoggingFilter`) that logs exceptions escaping Spring MVC at `ERROR` — and crucially
**while the trace context is still in the MDC**. It is ordered just inside Spring Boot's
`ServerHttpObservationFilter` but outside Spring Security and the error-page filter, so exceptions
those layers handle themselves are not double-logged. The exception is re-thrown unchanged, so
response behaviour is unaffected.

## Properties

| Property                                                | Default | Description                                                                |
|---------------------------------------------------------|---------|----------------------------------------------------------------------------|
| `jeap.logging.platform`                                 | (unset) | Selects the JSON appender: `cloudwatch` or `rhos`; unset → `consoletext`.  |
| `jeap.logging.rest.unhandled-exception-logging.enabled` | `false` | Register the servlet filter that logs unhandled MVC exceptions at `ERROR`. |

## Log levels (operational guidance)

| Level   | Use                                                                                                                                                                                                                                    |
|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ERROR` | Conditions that typically lead to an incident (systematically reviewed by DevOps). Errors caught by the jEAP error-handling service are **not** `ERROR` but `WARN`, possibly `INFO`.                                                   |
| `WARN`  | Recoverable problems, e.g. handled by the error-handling service.                                                                                                                                                                      |
| `INFO`  | Default level on acceptance (ABN) and production (PRD) is `INFO+`. Lower levels (`DEBUG`/`TRACE`) are enabled only situationally and temporarily; load/performance tests must run at `INFO+` to avoid swamping the log infrastructure. |

## Common pitfalls

- Setting `jeap.logging.platform` is what activates JSON output — a Spring profile name alone does not
  switch the format (only `rollingLogFile` is profile-driven).
- REST request lines only appear if the `ch.admin.bit.jeap.log.RestRequestTracer` logger is at
  `DEBUG`/`TRACE`; on PRD/ABN (`INFO+`) they are silent by design.
- The starter cannot mask payloads you pass yourself — never put tokens or credentials into a log
  message or its arguments.

## Related

- [jeap-spring-boot-monitoring-starter](jeap-spring-boot-monitoring-starter.md)
- [jeap-spring-boot-rest-request-tracing](jeap-spring-boot-rest-request-tracing.md)
- [jeap-spring-boot-application-starter](jeap-spring-boot-application-starter.md)
- [Configuration property reference](configuration.md)
- [jeap-spring-boot-starters](../README.md)
