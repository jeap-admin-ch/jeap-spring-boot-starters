# Application starter

`jeap-spring-boot-application-starter` is a composite starter for standard jEAP applications. It
bundles [`jeap-spring-boot-logging-starter`](jeap-spring-boot-logging-starter.md) and ships sensible
defaults for connection pooling, reverse-proxy handling and single-page-application (SPA) frontend
routing, so a typical SCS-style service (frontend + API from the same root context) does not have to
wire these concerns by hand.

## What it configures

### HikariCP pooling

`DbPoolingDefaultsEnvPostProcessor` (an `EnvironmentPostProcessor`, registered via `spring.factories`)
applies HikariCP defaults *when HikariCP is on the classpath*. Without it, Hikari would default to a
fixed pool of 10 connections; these defaults mirror Spring Cloud's `DataSourceConfigurer`. They are
applied as a lowest-precedence property source, so an application can still override any of them.

| Property                                     | Default                                               |
|----------------------------------------------|-------------------------------------------------------|
| `spring.datasource.hikari.maximum-pool-size` | `4`                                                   |
| `spring.datasource.hikari.minimum-idle`      | `0`                                                   |
| `spring.datasource.hikari.keepalive-time`    | `120000` (ms)                                         |
| `spring.datasource.hikari.pool-name`         | `hikari-cp` (suffixed with `spring.application.name`) |

### Reverse proxy / forward headers

`ReverseProxyPropertiesEnvPostProcessor` (also an `EnvironmentPostProcessor`) sets defaults suited to
jEAP apps deployed behind a load balancer / reverse proxy that performs TLS termination and sets the
standard forwarding headers:

| Property                               | Default  | Why                                                                                                                                                               |
|----------------------------------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `server.tomcat.use-relative-redirects` | `true`   | Avoids HTTPS→HTTP protocol downgrade when Tomcat redirects the context root (e.g. `/svc` → `/svc/`) and reduces forwarding-header confusion in multi-proxy chains |
| `server.forward-headers-strategy`      | `NATIVE` | Makes Spring reconstruct the original request URL from `X-Forwarded-*` headers                                                                                    |

Relative redirects are the official jEAP default (from `jeap-spring-boot-parent` 18.2.0 onward): when a
context-root redirect was sent as an absolute URL, Tomcat did not honour `X-Forwarded-Proto` and
downgraded the scheme to HTTP. Emitting a relative `Location` (e.g. `/my-service/target`) fixes both
the protocol-downgrade and the wrong-external-host problems. Both defaults can be overridden.

### Frontend route handling

`FrontendRouteRedirectExceptionHandler` is a `@ControllerAdvice` (extending
`ResponseEntityExceptionHandler`) that catches `NoResourceFoundException` and serves
`classpath:/static/index.html` with HTTP 200 for requests that look like an SPA route, so deep links /
bookmarks to client-side routes resolve.

Since Spring 6.1 / Spring Boot 3.2 an unmatched request throws `NoResourceFoundException` instead of
returning a plain 404, which broke the common "serve index.html on 404" workaround for direct frontend
route navigation. This handler restores it. `FrontendRouteMatcher` decides whether a request looks like
a frontend route: it must **not** contain a dot (otherwise it is treated as a file like `/app.css`) and
its first path segment must **not** contain a known non-frontend part (`api`, `actuator` by default).
Non-matching requests fall through to a normal 404.

> The exception handler is **not** auto-registered. Register it explicitly — either as a bean, or by
> extending it from your own `@ControllerAdvice`:

```java
// Register the jEAP advice as a bean (no custom advice needed)
@Bean
public FrontendRouteRedirectExceptionHandler frontendRouteRedirectExceptionHandler() {
    return new FrontendRouteRedirectExceptionHandler();
}

// - or - extend it to add your own exception handlers
@ControllerAdvice
public class MyControllerAdvice extends FrontendRouteRedirectExceptionHandler {
    // ... your own @ExceptionHandler methods
}
```

Pass a custom set of non-frontend path parts to the constructor to override the `api`/`actuator`
defaults.

### Other defaults

The starter also raises `server.max-http-request-header-size` to `64KB` (large auth headers / tokens).

## Key classes

- `DbPoolingDefaultsEnvPostProcessor` — HikariCP defaults (classpath-conditional)
- `ReverseProxyPropertiesEnvPostProcessor` — relative redirects + `NATIVE` forward headers
- `FrontendRouteRedirectExceptionHandler` / `FrontendRouteMatcher` — SPA deep-link handling

## Pitfalls

- The env-post-processor defaults (pooling, reverse proxy) apply automatically; the frontend route
  handler does **not** — you must declare it yourself.
- `index.html` must be served from `classpath:/static/index.html` for the route handler to work.
- The pooling defaults only apply when HikariCP is on the classpath.

## Related

- [jeap-spring-boot-logging-starter](jeap-spring-boot-logging-starter.md)
- [jeap-spring-boot-web-config-starter](jeap-spring-boot-web-config-starter.md)
- [Configuration property reference](configuration.md)
- [jeap-spring-boot-starters](../README.md)
