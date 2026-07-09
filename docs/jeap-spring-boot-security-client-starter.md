# Security client starter

`jeap-spring-boot-security-client-starter` configures a service as an OAuth2 **client** so it can call
other secured REST APIs (horizontal, service-to-service authentication). It builds on Spring
Security's OAuth2 client support and provides preconfigured `RestClient.Builder` instances that
automatically attach an OAuth2 access token to outgoing requests. The protected side is covered by
[`jeap-spring-boot-security-starter`](jeap-spring-boot-security-starter.md).

There are two horizontal cases:

- **System context** — the service calls a downstream service with *its own* token, obtained via the
  OAuth2 client-credentials flow. Used for genuine system-to-system calls, including calls a user
  would not be allowed to make directly.
- **Context propagation** — the service reuses the token it *received* from its caller (e.g. a
  Backend-for-Frontend forwarding the original user's token), so the downstream service authorizes
  the request in the original caller's context.

## Add it

```xml
<dependency>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-spring-boot-security-client-starter</artifactId>
</dependency>
```

This starter comes transitively with `jeap-spring-boot-security-starter`; since version 17.43.0 it is
also available as the standalone dependency above. It supports the Spring WebMvc stack. The jEAP
guideline is to use the Spring `RestClient` (introduced in Spring 6.1); `RestTemplate` is not
supported.

## Configuration

Client registrations and providers use the standard Spring Security OAuth2 client properties — this
starter has no `@ConfigurationProperties` of its own. To call an API in the system context, configure
a client-credentials registration and its provider:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          default:
            provider: default
            client-id: my-service
            client-secret: ${MY_CLIENT_SECRET}
            authorization-grant-type: client_credentials
        provider:
          default:
            issuer-uri: https://keycloak.example.ch/auth/realms/system
```

| Property                                                                    | Value                                                  |
|-----------------------------------------------------------------------------|--------------------------------------------------------|
| `spring.security.oauth2.client.registration.<reg>.client-id`                | Application's client id in the auth server             |
| `spring.security.oauth2.client.registration.<reg>.client-secret`            | Application's client secret                            |
| `spring.security.oauth2.client.registration.<reg>.authorization-grant-type` | `client_credentials`                                   |
| `spring.security.oauth2.client.registration.<reg>.provider`                 | Name of a provider under `...client.provider.<name>.*` |
| `spring.security.oauth2.client.provider.<name>.issuer-uri`                  | Issuer URI of the auth server (or set `token-uri`)     |

The starter's OAuth2 client functionality activates only when such a client registration is present
(i.e. when Spring auto-creates the `ClientRegistrationRepository` and `OAuth2AuthorizedClientService`
beans). To call APIs whose access is managed by different authorization servers, configure one
provider per server and point each registration at the right provider via its `provider` field.

The `OAuth2ClientRegistryPostprocessor` (`ch.admin.bit.jeap.security.client`) defaults the OIDC scope
to `openid` for any registration that declares no scope, so Spring Security does not fetch all scopes
from the auth server's configuration endpoint.

## Building a RestClient

Inject `JeapOAuth2RestClientBuilderFactory` (`ch.admin.bit.jeap.security.restclient`) and create a
`RestClient.Builder` for the desired flow, then configure it (e.g. a base URL) before building:

```java
@RequiredArgsConstructor
class MyApiClient {
    private final JeapOAuth2RestClientBuilderFactory clientBuilderFactory;

    RestClient restClient() {
        return clientBuilderFactory
                .createForClientRegistryId("default")   // client-credentials token, system context
                .baseUrl("https://other-service.example.ch")
                .build();
    }
}
```

The factory offers three strategies; each returns a `RestClient.Builder`:

| Method                                                            | Token used                                                                            | Context                          |
|-------------------------------------------------------------------|---------------------------------------------------------------------------------------|----------------------------------|
| `createForClientRegistryId(id)`                                   | A fresh token via the named client-credentials registration                           | System                           |
| `createForTokenFromIncomingRequest()`                             | The token of the current incoming request, forwarded as-is; no token if there is none | Context propagation (or none)    |
| `createForClientRegistryIdPreferringTokenFromIncomingRequest(id)` | The incoming request token if present, otherwise the client-credentials token         | Context propagation, else system |

Use the incoming-request variants for a Backend-for-Frontend that should call downstream services in
the original user's context; use `createForClientRegistryId(id)` for genuine system-to-system calls
that need the service's own permissions.

## Testing

In tests, swap the factory for `MockJeapOAuth2RestClientBuilderFactory`
(`ch.admin.bit.jeap.security.test.client`), whose built clients take their access token from a settable
`AuthTokenProvider` instead of a real authorization server. See
[`jeap-spring-boot-security-starter-test`](jeap-spring-boot-security-starter-test.md).

## Related

- [jeap-spring-boot-security-starter](jeap-spring-boot-security-starter.md)
- [jeap-spring-boot-security-starter-test](jeap-spring-boot-security-starter-test.md)
- [Configuration property reference](configuration.md)
- [jeap-spring-boot-starters](../README.md)
