# REST request tracing

`jeap-spring-boot-rest-request-tracing` provides low-level servlet request/response tracing with header
masking. Request tracing helps understand what happens on a system: for each incoming HTTP call it
records who called what and how the service responded, and routes that information to trace listeners.
It is the technology-neutral core consumed by the higher-level REST request *logging* in
[`jeap-spring-boot-logging-starter`](jeap-spring-boot-logging-starter.md), which turns the trace events
into the structured `RestRequestTracer` log statements.

A traced response carries: HTTP method, request URI and matched URI pattern, status code, the calling
application (`caller`), the authenticated user, elapsed time (`dt`/ms), remote address, the (filtered)
request and response headers, and whitelisted request attributes.

## How it works

`TracerConfiguration` (`@AutoConfiguration`, `@ConditionalOnWebApplication`, prefix `jeap.rest.tracing`)
loads `tracer.properties`, binds the properties and `@ComponentScan`s the module to wire the tracer and
its servlet filters:

- **`RestRequestTracer`** — builds `RestRequestTrace` (on request) and `RestResponseTrace` (on response)
  events and dispatches them to all `RestRequestListener` / `RestResponseListener` beans. It short-
  circuits when no active listener is present, applies header masking/blacklisting and attribute
  filtering, and extracts the caller from the `JEAP-APPLICATION-NAME` request header. Listener
  exceptions are caught and logged so a listener never breaks the request.
- **`ServletRequestTracer`** — a high-precedence (`HIGHEST_PRECEDENCE + 10`) `OncePerRequestFilter` that
  traces the incoming request and, in a `finally` block, the response (status, headers, attributes,
  elapsed time). Async dispatches are skipped.
- **`ServletRequestSecurityTracer`** — a lowest-precedence filter that traces the request *after* the
  security context is established (so authentication is known), emitting `RestResponseSecurityTrace` to
  an optional `RestSecurityResponseListener`. It skips `uri-filter-pattern` matches and 401/403
  responses.
- **`ServletStoreUserFilter`** — present only when the jEAP security `JeapAuthenticationToken` is on the
  classpath; stores the token subject as a request attribute so `RestRequestTracer` can report `user`.
- **`AddSenderSystemHeaderToRestClient`** — a `RestClientCustomizer` (when `RestClient.Builder` is on
  the classpath) that adds the `JEAP-APPLICATION-NAME` header (value = `application-name`) to every
  outgoing `RestClient` call, which is how the receiving service learns the `caller`.

## Consuming traces

Implement and register `RestRequestListener` and/or `RestResponseListener` beans. Both expose an
`isRequest/ResponseListenerActive()` gate so the tracer can avoid building events when no listener is
interested (e.g. when the corresponding log level is disabled). The logging starter ships listeners
that emit the structured trace log; provide your own to feed traces elsewhere.

## Header masking & filtering

For each request/response header the tracer applies, in order:

- **blacklist** (`header-blacklist`) — header dropped from the trace entirely (prefix match,
  case-insensitive);
- **mask** (`header-masked`) — header emitted with each value replaced by `***`.

Request attributes are included only if their name matches a prefix in `attributes-whitelist`.

## Properties

Prefix: `jeap.rest.tracing` (defaults from `tracer.properties`).

| Property                           | Type           | Default                                                           | Description                                            |
|------------------------------------|----------------|-------------------------------------------------------------------|--------------------------------------------------------|
| `header-masked`                    | `List<String>` | `Authorization, Cookie, Set-Cookie, Set-Cookie2, x-jwt-assertion` | Headers whose values are masked as `***`               |
| `header-blacklist`                 | `List<String>` | (empty)                                                           | Headers excluded from traces entirely                  |
| `attributes-whitelist`             | `List<String>` | `org.springframework.web.servlet.HandlerMapping`                  | Request-attribute name prefixes to include             |
| `application-name`                 | `String`       | `${spring.application.name}`                                      | Name sent as `JEAP-APPLICATION-NAME` on outgoing calls |
| `uri-filter-pattern`               | `Pattern`      | `.*/actuator/.*`                                                  | URIs excluded from security tracing                    |
| `full-response-details-in-message` | `boolean`      | `false`                                                           | Include full response details in the trace message     |

Whether trace *logs* are produced is governed by the log level of the tracer logger (the logging
starter logs after responding at `DEBUG` and additionally on receipt at `TRACE`):

```yaml
logging:
  level:
    ch.admin.bit.jeap.log.RestRequestTracer: DEBUG # or TRACE
```

## Pitfalls

- This module emits trace *events* only — without the logging starter (or a custom listener) nothing
  is logged.
- `caller` is populated only when the upstream service also runs this tracing (it sets the
  `JEAP-APPLICATION-NAME` header on its `RestClient` calls).
- `user` is populated only when the jEAP security starter is present (the `JeapAuthenticationToken`
  class drives `ServletStoreUserFilter`).
- Always keep sensitive headers in `header-masked` / `header-blacklist`; the defaults cover auth and
  cookies but not custom secret headers.

## Related

- [jeap-spring-boot-logging-starter](jeap-spring-boot-logging-starter.md)
- [Configuration property reference](configuration.md)
- [jeap-spring-boot-starters](../README.md)
