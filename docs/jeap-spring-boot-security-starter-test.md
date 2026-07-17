# Security starter test support

`jeap-spring-boot-security-starter-test` provides test support for services secured with
[`jeap-spring-boot-security-starter`](jeap-spring-boot-security-starter.md). All authorization the
security starter offers is driven by the `JeapAuthenticationToken` in the `SecurityContext`, so to
test authorization a test must supply one. This module lets tests do that at every level — authenticate
MockMvc requests, place a token directly in the `SecurityContext`, mint signed JWTs against a test key
pair, serve a mock JWKS endpoint for full integration tests, and mock the authorization beans.

Add it with test scope:

```xml
<dependency>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-spring-boot-security-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

In a Spring Boot test this module auto-activates `PermitAllWebSecurityConfiguration` (a high-priority
chain that lets all requests through), overriding the security starter's default protection. This is
what you want when the test sets the `Authentication` itself (e.g. MockMvc). Import
`DisableJeapPermitAllSecurityConfiguration` to suppress it, or `DisableJeapSecurityStarterAutoConfiguration`
to disable the security starter's auto-configuration entirely.

## WireMock Spring Boot integration

The test-support module includes WireMock's official Spring Boot integration. It uses the standalone
distribution so WireMock's Jetty dependencies do not conflict with the application's dependency
versions. Consumers do not need an additional WireMock dependency or a `wiremock-jetty12` exclusion.

Spring Cloud 2025.1 removed `spring-cloud-contract-wiremock`. Replace its
`@AutoConfigureWireMock` annotation with `@EnableWireMock`; a random port is used by default:

```java
import com.github.tomakehurst.wiremock.WireMockServer;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@SpringBootTest
@EnableWireMock
class MyIntegrationTest {

    @InjectWireMock
    WireMockServer wireMockServer;

    @Value("${wiremock.server.baseUrl}")
    String wireMockBaseUrl;
}
```

The integration publishes `wiremock.server.baseUrl` and `wiremock.server.port`. Use
`@ConfigureWireMock` inside `@EnableWireMock` to change these property names, configure a fixed port,
load mappings or run named WireMock instances.

## Which approach for which test

| Test kind                                                                 | Supply the token via                                                                              |
|---------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| MockMvc integration test on a controller                                  | `@WithJeapAuthenticationToken`, or set the `Authentication` on the request                        |
| Component/service integration test (no HTTP)                              | `JeapAuthenticationTestTokenBuilder` placed in the `SecurityContext`                              |
| Unit test of programmatic authorization                                   | A mock authorization bean (`ServletSimpleAuthorizationMock` / `ServletSemanticAuthorizationMock`) |
| Full security integration test (through the resource-server filter chain) | A signed JWT from `JwsBuilder` + the mock JWKS endpoint                                           |

## Authenticating MockMvc tests

Annotate a test with `@WithJeapAuthenticationToken` (`ch.admin.bit.jeap.security.test`) to populate
the `SecurityContext` with a `JeapAuthenticationToken`:

```java
@Test
@WithJeapAuthenticationToken(userRoles = {"admin"}, bpRoles = {"partner-1=reader,writer"})
void getData_isAuthorized() throws Exception {
    mockMvc.perform(get("/api/data")).andExpect(status().isOk());
}
```

Annotation attributes (all optional):

| Attribute     | Type       | Default         | Description                                             |
|---------------|------------|-----------------|---------------------------------------------------------|
| `userRoles`   | `String[]` | `{}`            | Roles independent of any business partner (`userroles`) |
| `bpRoles`     | `String[]` | `{}`            | Business-partner roles, each as `"bpId=role1,role2"`    |
| `username`    | `String`   | `"username"`    | `preferred_username` claim                              |
| `givenName`   | `String`   | `"givenName"`   | `given_name` claim                                      |
| `familyName`  | `String`   | `"familyName"`  | `family_name` claim                                     |
| `displayName` | `String`   | `"displayName"` | `name` claim                                            |
| `extId`       | `String`   | `"extId"`       | `ext_id` claim                                          |

## Building a token directly

`JeapAuthenticationTestTokenBuilder` (`ch.admin.bit.jeap.security.test.resource`) builds a
`JeapAuthenticationToken` programmatically — useful when you place it in the `SecurityContext`
yourself for component or service tests:

```java
JeapAuthenticationToken token = JeapAuthenticationTestTokenBuilder.create()
        .withContext(JeapAuthenticationContext.USER)
        .withUserRoles("admin")
        .withBusinessPartnerRoles("partner-1", "reader")
        .withPreferredUsername("jane")
        .build();
```

Start with `create()` or `createWithJwt(Jwt jwt)`. Builder methods include `withContext(...)`,
`withUserRoles(...)`, `withBusinessPartnerRoles(businessPartner, ...)` (both accept plain `String`
roles or `SemanticApplicationRole` values), `withAuthorities(...)`, `withClaim(name, value)`,
`withSubject(...)`, `withExtId(...)`, `withAdminDirUID(...)`, `withName(...)`, `withGivenName(...)`,
`withFamilyName(...)`, `withPreferredUsername(...)` and `withLocale(...)`. `build()` derives the
`GrantedAuthority`s from the roles unless `withAuthorities(...)` set them explicitly.

## Minting signed JWTs

For full integration tests that go through the real resource-server filter chain, mint a signed JWT
with `JwsBuilder` (`ch.admin.bit.jeap.security.test.jws`). Use a factory method —
`create(jwtId, issuer, expiry, notBefore, issuedAt, subject, context)`,
`createValidFromNow(subject, context, validity, temporalUnit)` or
`createValidForFixedLongPeriod(subject, context)` — then chain `withIssuer(...)`, `withAudiences(...)`,
`withUserRoles(...)`, `withBusinessPartnerRoles(...)`, `withClaim(...)`, `withRsaKey(...)` and `build()`
to obtain a `SignedJWT`. Useful constants: `DEFAULT_ISSUER = http://localhost/auth`,
`B2B_ISSUER = http://localhost/b2b/auth`.

`JwsBuilderFactory` pre-populates a `JwsBuilder` with the test auth-server signing key, so the token
matches the mock JWKS endpoint: `createBuilder(...)`, `createValidFromNowBuilder(...)` and
`createValidForFixedLongPeriodBuilder(...)` mirror the static factory methods above. Prefer these
when serving the mock JWKS endpoint.

## Mock JWKS endpoint and test keys

For end-to-end token validation, import `JeapOAuth2IntegrationTestConfiguration`
(`ch.admin.bit.jeap.security.test.configuration`) — or the resource-only
`JeapOAuth2IntegrationTestResourceConfiguration`. It registers `ServletJwksEndpointMock`, a
`@RestController` serving the JWKS at `/.well-known/jwks.json`, plus the JWS test support. Point the
resource server at it and use a matching issuer:

```yaml
jeap:
  security:
    oauth2:
      resourceserver:
        authorization-server:
          issuer: http://localhost/auth
          jwk-set-uri: http://localhost:${local.server.port}/.well-known/jwks.json
```

Then send the token as a bearer header (e.g. with REST Assured: `given().auth().oauth2(token)...`).
The RSA test key pair is provided by `TestKeyProvider`, configured under
`jeap.oauth2.test-key-provider.auth-server-key`:

| Property                | Default                                             |
|-------------------------|-----------------------------------------------------|
| `keytore-resource-path` | `classpath:/testkeys/default-rsa-test-key-pair.p12` |
| `keystore-type`         | `pkcs12`                                            |
| `keytore-password`      | `secret`                                            |
| `key-alias`             | `default-test-key`                                  |

> **Note:** `keytore-resource-path` and `keytore-password` are spelled as shown on purpose — the
> underlying fields in `TestKeyProviderConfigurationProperties` are misspelled (`keytore…`), so these
> are the names that actually bind. The `keystore-…` spelling would be silently ignored.

`RSAKeyUtils` (`ch.admin.bit.jeap.security.test.jws`) loads an RSA key from a keystore or generates a
fresh 2048-bit key, for use with `JwsBuilder` or the JWKS mock.

## OIDC Authorization Code mock server

For tests that need discovery + authorize + token + userinfo endpoints, use
`OidcAuthorizationMockServer` (`ch.admin.bit.jeap.security.test.mock`). It runs a WireMock-backed
OIDC provider with Authorization Code + PKCE support and signs tokens with the module's shared
`JwsBuilder` / `RSAKeyUtils`.

```java
OidcAuthorizationMockServer mockServer = OidcAuthorizationMockServer
        .builder(18081, "/mock-idp", "http://localhost:18080")
        .withDefaultClientId("my-client-id")
        .withGivenName("Max")
        .withFamilyName("Muster")
        .withLocale("de")
        .withUserRoles(List.of("jeap_@mysystem_#read"))
        .withRoleProfile("admin", List.of("jeap_@mysystem_#write"))
        .build();

mockServer.start();
```

You can switch roles per test without starting another server instance:

- set the active profile in test code: `mockServer.setActiveProfile("admin")`;
- call `mockServer.reset()` between tests to clear runtime state and restore the default profile.

Claim handling:

- `sub` can be configured via `withSubject(...)`; `aud` is derived from `client_id` (or `withDefaultClientId(...)`).
- OIDC standard identity claims can be set via `withName(...)`, `withGivenName(...)`, `withFamilyName(...)`, `withPreferredUsername(...)` and `withLocale(...)`.
- Use `withAccessTokenClaims(...)`, `withIdTokenClaims(...)`, `withUserInfoClaims(...)` for additional custom claims.

Endpoints under the configured base path:

- `/.well-known/openid-configuration`
- `/.well-known/jwks.json`
- `/oauth2/authorize`
- `/oauth2/token`
- `/oauth2/userinfo`

## Mock authorization beans

To unit-test programmatic authorization without a token, inject a mock authorization bean — each
extends the real bean and answers all queries from the roles you build in:

- `ServletSimpleAuthorizationMock` (extends `ServletSimpleAuthorization`) —
  `builder().userRole(...).businessPartnerRole(bpId, roles).build()`;
- `ServletSemanticAuthorizationMock` (extends `ServletSemanticAuthorization`) —
  `builder().systemName(...).userRole(...).businessPartnerRole(bpId, roles).build()` (system name required).

Both live in `ch.admin.bit.jeap.security.test.resource`. For Spring Boot tests run *without* security,
import `DisableJeapSecurityStarterAutoConfiguration` and define/mock the `jeapAuthorization` bean only
if the application accesses it programmatically (not needed when it is used only in `@PreAuthorize`).

## Mock client factory

`MockJeapOAuth2RestClientBuilderFactory` (`ch.admin.bit.jeap.security.test.client`) is a mock of the
[client starter](jeap-spring-boot-security-client-starter.md)'s `JeapOAuth2RestClientBuilderFactory`.
Its clients take their access token from a settable `AuthTokenProvider` rather than a real
authorization server; load a `JwsBuilder`-minted token into the provider per test.

## Related

- [jeap-spring-boot-security-starter](jeap-spring-boot-security-starter.md)
- [jeap-spring-boot-security-client-starter](jeap-spring-boot-security-client-starter.md)
- [Configuration property reference](configuration.md)
- [jeap-spring-boot-starters](../README.md)
