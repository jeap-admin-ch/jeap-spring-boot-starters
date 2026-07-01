# Web config starter

`jeap-spring-boot-web-config-starter` adds security and caching HTTP headers to the responses a jEAP
service serves to a browser. It targets services that ship a single-page-application (SPA) frontend
together with their backend API from the same root context (the jEAP Blueprint Microservice / SCS
shape): the starter hardens responses with a restrictive `Content-Security-Policy` and related
security headers, and applies caching rules tuned for SPA assets.

By default Spring Boot only sets a handful of headers via Spring Security (and only for secured
resources). This starter fills that gap with configurable, secure-by-default headers applied through a
servlet filter, without requiring each service to wire the headers by hand. It is intended for SCS
applications; it is *not* recommended for differently structured apps (e.g. Spring Boot Admin UIs).

## How it works

`HeaderConfiguration` is the `@AutoConfiguration` + `@ConfigurationProperties(prefix = "jeap.web.headers")`
class; it also loads `jeap-web-header-defaults.properties` via `@PropertySource`. `ServletWebConfiguration`
(active for `SERVLET` web applications) registers the filters:

- **`AddHeadersFilter`** — a `OncePerRequestFilter` that, for `GET`/`HEAD` requests whose path is
  accepted (see path matching below), delegates to `ServletHeaders` (a subclass of `AbstractHeaders`)
  to set the security and caching headers. Header-setting failures are caught and logged at WARN so a
  filter error never breaks the response.
- **`SseAwareEtagHeaderFilter`** — a subclass of Spring's `ShallowEtagHeaderFilter` that adds an `ETag`
  to static resources so the browser can skip re-downloading unchanged bodies. It overrides
  `shouldNotFilter` to skip Server-Sent-Events (`Accept: text/event-stream`) responses, which must not
  be buffered. Registered unless `jeap.web.headers.etag=false`.
- **`disableEtagCachingOnForwardFilter`** — runs on `FORWARD`/`INCLUDE` dispatches and calls
  `ShallowEtagHeaderFilter.disableContentCaching(...)`. This is needed because the ETag filter buffers
  the response; on a `forward:` to a static resource (e.g. the welcome-page forward of `/` to
  `index.html`) the container commits the response before control returns to the buffering filter,
  which would otherwise yield an empty body. Also gated by `jeap.web.headers.etag`.

### Security headers

Set by `AbstractHeaders.addSecurityHeaders`:

| Header                      | Default value                                                                                                                                                            |
|-----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Content-Security-Policy`   | `default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; font-src 'self'; img-src 'self'; connect-src 'self'; frame-src 'self'; frame-ancestors 'self'` |
| `Strict-Transport-Security` | `max-age=16070400; includeSubDomains` (HSTS; ignored by browsers over plain HTTP / on `localhost`)                                                                       |
| `Feature-Policy`            | `microphone 'none'; payment 'none'; camera 'none'`                                                                                                                       |
| `X-Content-Type-Options`    | `nosniff`                                                                                                                                                                |
| `Referrer-Policy`           | `strict-origin-when-cross-origin`                                                                                                                                        |
| `X-Frame-Options`           | `sameorigin` (kept for backwards compatibility; superseded by CSP `frame-ancestors`)                                                                                     |

The default CSP is intentionally very restrictive — many real frontends will need adjustments (e.g.
they load resources from `*.admin.ch` or use the ePortal). The recommended workflow is to run the UI
with the default policy and fix the violations reported in the browser console by adjusting the app or
the `content-security-policy` value.

The `connect-src` and `frame-src` directives are automatically extended with the origin of every entry
in `additional-content-sources`; the default seeds this from the OAuth2 issuer
(`jeap.security.oauth2.resourceserver.authorization-server.issuer`) so API calls to the identity
provider and silent token refresh in an iframe keep working. URL values are reduced to their origin
(scheme + host + port); literal (`'self'`) and scheme (`blob:`) sources are passed through unchanged.

### Caching headers

Set by `AbstractHeaders.addCachingHeaders`, keyed on the request path suffix:

| Path ends with        | `Cache-Control`                                         | Rationale                                                          |
|-----------------------|---------------------------------------------------------|--------------------------------------------------------------------|
| `.html`, `.json`, `/` | `no-cache` (`Expires: 0`)                               | The single `index.html` and translation JSON must never be stale   |
| `.js`, `.css`         | `public, max-age=15778476, must-revalidate` (~6 months) | Build tools hash these filenames, so a new version means new files |
| anything else         | `public, max-age=604800, must-revalidate` (1 week)      | Other static resources change rarely but stay revalidated          |

## Path matching

`HeaderConfiguration.accept(path)` decides per request. Headers are added when the path matches an
accept rule **and** does not match a skip rule. If no accept prefixes/patterns are configured it
defaults to accept (secure by default). Skip rules: `skipPathPrefixes` (default `/api` plus the
actuator base path), `skipPathSuffixes` (default `-api`), and optional `acceptPathPattern` /
`skipPathPattern` regexes.

## Customization hook

Provide a bean implementing `HttpHeaderFilterPostProcessor` to mutate the computed header map per
request (its `postProcessHeaders(headers, method, path)` runs after the security and caching headers
are computed but before they are written). Absent such a bean, a `NO_OP` is used.

## Properties

Prefix: `jeap.web.headers`.

| Property                                      | Default                                            | Description                                                     |
|-----------------------------------------------|----------------------------------------------------|-----------------------------------------------------------------|
| `jeap.web.headers.additional-content-sources` | OAuth2 issuer (if set)                             | Extra origins added to CSP `connect-src` / `frame-src`          |
| `jeap.web.headers.content-security-policy`    | (built-in default, see above)                      | Overrides the whole CSP header value                            |
| `jeap.web.headers.feature-policy`             | `microphone 'none'; payment 'none'; camera 'none'` | Overrides the Feature-Policy header                             |
| `jeap.web.headers.skip-path-prefixes`         | `/api`, actuator base path                         | Path prefixes that receive no headers                           |
| `jeap.web.headers.skip-path-suffixes`         | `-api`                                             | First-segment suffixes that receive no headers                  |
| `jeap.web.headers.accept-path-prefixes`       | (empty)                                            | If set, only matching paths get headers                         |
| `jeap.web.headers.accept-path-pattern`        | (none)                                             | Regex; if set, matching paths get headers                       |
| `jeap.web.headers.skip-path-pattern`          | (none)                                             | Regex; matching paths receive no headers                        |
| `jeap.web.headers.etag`                       | `true`                                             | Register the `ShallowEtagHeaderFilter` (set `false` to disable) |

```yaml
jeap:
  web:
    headers:
      content-security-policy: >-
        default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline';
        font-src 'self'; img-src 'self' data:; connect-src 'self' https://login.example.admin.ch;
        frame-src 'self' https://login.example.admin.ch; frame-ancestors 'self'
      additional-content-sources:
        - https://assets.example.admin.ch
```

## Key classes

- `HeaderConfiguration` — auto-config + properties + path-acceptance logic
- `ServletWebConfiguration` — registers `AddHeadersFilter`, the ETag filters
- `AbstractHeaders` / `ServletHeaders` — compute and write the headers
- `SseAwareEtagHeaderFilter` — ETag filter that skips SSE
- `HttpHeaderFilterPostProcessor` — per-request header customization hook

## Pitfalls

- The default CSP breaks most non-trivial frontends — plan to override `content-security-policy`.
- Headers are only applied to `GET`/`HEAD`; `/api` and `-api` paths are skipped by default, so don't
  rely on this starter to harden API responses.
- HSTS and the secure defaults assume the app is reached over HTTPS (typically terminated at a reverse
  proxy); over plain `localhost` HSTS is ignored by browsers.

## Related

- [jeap-spring-boot-application-starter](jeap-spring-boot-application-starter.md)
- [Configuration property reference](configuration.md)
- [jeap-spring-boot-starters](../README.md)
