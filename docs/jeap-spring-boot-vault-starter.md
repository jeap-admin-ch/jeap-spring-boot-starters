# Vault starter

`jeap-spring-boot-vault-starter` integrates HashiCorp Vault into a jEAP Spring Boot service via
Spring Cloud Vault, so that secrets and certificates can be managed centrally instead of being baked
into the application's configuration files.

## Why externalise secrets and certificates

In a microservice architecture, sensitive configuration data raises problems that ordinary
profile-based property files cannot solve well:

- **Restricted access** — credentials, API keys and similar data must only be readable by the
  services that need them; this is hard to enforce with config files that are part of the build.
- **Independent lifecycle** — secrets and certificates rotate on their own schedule. Replacing a
  certificate or rolling a credential should not require building and deploying a new version of the
  service.
- **Central auditability** — changes to sensitive configuration should be traceable (who changed
  what, and why) in one place rather than scattered across per-service files.

The starter solves this by reading secrets from a central Vault instance at application startup
(and, where needed, at runtime through the Vault API). Configuration uses jEAP shortcut properties
under `jeap.vault.*`, which the starter maps onto the standard `spring.cloud.vault.*` properties so
that teams do not have to know the full Spring Cloud Vault property surface.

## How it works

The starter contributes its base configuration through an `EnvironmentPostProcessor`
(`VaultBasePropertiesEnvPostProcessor`, registered in `spring.factories`) that runs *before* Spring's
`ConfigDataEnvironmentPostProcessor`, so the Vault config import is processed with the jEAP defaults
already in place. Two property templates are bundled:

- `jeap-vault-starter-base.properties` — used in non-Kubernetes environments; selects **AppRole**
  authentication.
- `jeap-vault-starter-rhos-base.properties` — used when the Kubernetes cloud platform is detected
  (RHOS / OpenShift); selects **Kubernetes** authentication.

The post-processor only fills in properties that are **not already set**, so any value can be
overridden by the application, and re-running the post-processor (bootstrap phase, then context
initialization) never resets values back to the defaults. If `IS_INIT_CONTAINER_EXECUTION` is set
(database-migration job container) no Vault base properties are added. Setting
`spring.cloud.vault.enabled=false` disables the whole integration — handy for local development,
where secrets can be supplied as ordinary properties instead.

Two conditional auto-configurations (`JeapVaultAppRoleConfigLogger`, `JeapVaultKubernetesConfigLogger`)
log the effective Vault URL, auth method and secret path once at startup, which is useful for
diagnosing misconfiguration. A `SuppressInvalidCookieHeaderWarningEnvPostProcessor` silences a noisy
Apache HTTP client cookie warning produced by the web application firewall in front of the BIT Vault
instances.

## Authentication methods

### AppRole (default, non-Kubernetes / AWS)

The service authenticates with a `role-id` and a `secret-id`. The KV v2 secret backend is mounted at
`secret/{system-name}`, the AppRole backend at `approle/{system-name}`, and a `shared` default
context holds cross-service secrets. `fail-fast` is enabled, so the application fails to start if
secrets cannot be retrieved.

```yaml
spring:
  application:
    name: my-service           # secrets looked up at secret/{system-name}/my-service
jeap:
  vault:
    url: https://vault.example.admin.ch
    system-name: jme           # -> secret/jme and approle/jme
    app-role:
      role-id: "${aws.secrets.app-role-id}"
      secret-id: "${aws.secrets.app-role-secret-id}"
```

On AWS the `role-id` / `secret-id` are themselves secrets: store them in AWS Secrets Manager and
import them with `jeap-spring-boot-config-aws-starter`, then reference them as shown above. They can
also be read with the Vault CLI:

```shell
vault read  auth/approle/${system-name}/role/${service-name}/role-id
vault write -f auth/approle/${system-name}/role/${service-name}/secret-id
```

### Kubernetes (RHOS / OpenShift)

On Kubernetes the service authenticates with the pod's service-account JWT (mounted at
`/var/run/secrets/kubernetes.io/serviceaccount/token`), so no `secret-id` has to be distributed. The
KV backend and the Kubernetes role/path must be configured explicitly:

```yaml
jeap:
  vault:
    url: https://vault-xxxx.apps.<cluster>.cloud.admin.ch
    kv:
      backend: bit-jme-d                 # RHOS namespace = KV backend
    kubernetes:
      role: my-service-role
      kubernetes-path: jwt-<cluster>     # mount path of the Kubernetes auth backend
```

This is the more advanced of the two RHOS approaches; for purely static secrets the platform's
out-of-the-box provisioned secret ConfigMap is usually sufficient, and the Vault starter is only
needed when secrets change at runtime or the Vault API is used directly (e.g. the transit engine).

## Configuration

The `jeap.vault.*` shortcut properties below are mapped onto `spring.cloud.vault.*`. Standard Spring
Cloud Vault properties may also be set directly to override defaults.

| Property                                | Auth method | Description                                                                  |
|-----------------------------------------|-------------|------------------------------------------------------------------------------|
| `jeap.vault.url`                        | both        | Vault server URL (maps to `spring.cloud.vault.uri`)                          |
| `jeap.vault.system-name`                | AppRole     | System identifier; builds `secret/{system-name}` and `approle/{system-name}` |
| `jeap.vault.app-role.role-id`           | AppRole     | AppRole role id                                                              |
| `jeap.vault.app-role.secret-id`         | AppRole     | AppRole secret id                                                            |
| `jeap.vault.kv.backend`                 | Kubernetes  | Mount path of the KV secrets backend to read from                            |
| `jeap.vault.kv.default-context`         | Kubernetes  | Default (shared) context name                                                |
| `jeap.vault.kubernetes.role`            | Kubernetes  | Vault role used for Kubernetes authentication                                |
| `jeap.vault.kubernetes.kubernetes-path` | Kubernetes  | Mount path of the Kubernetes auth backend in Vault                           |
| `spring.cloud.vault.enabled`            | both        | Set to `false` to disable Vault (e.g. local development)                     |

Defaults applied by the starter: `spring.cloud.vault.fail-fast=true`, KV v2 enabled, generic backend
disabled; the AppRole base also sets `kv.default-context=shared`.

## Common patterns and pitfalls

- **Local development** — set `spring.cloud.vault.enabled: false` and define the same property keys
  locally; the Vault base properties are then skipped entirely.
- **Importing secrets** — once Vault is active, secrets at `secret/{system-name}/{app-name}` and
  `secret/{system-name}/shared` are injected into the Spring `Environment` and can be referenced like
  any other property (e.g. `@Value`, `@ConfigurationProperties`).
- **Fail-fast** — a missing secret stops startup by design; verify the AppRole path / Kubernetes
  role and the KV backend mount if startup fails.
- **Runtime Vault use** — for the transit (crypto) engine and other live Vault API access, combine
  this starter with `jeap-crypto-vault-starter` rather than only reading static secrets at startup.

## Related

- [jeap-spring-boot-postgresql-aws-starter](jeap-spring-boot-postgresql-aws-starter.md)
- [jeap-spring-boot-object-storage-starter](jeap-spring-boot-object-storage-starter.md)
- [Configuration property reference](configuration.md)
- [jeap-spring-boot-starters](../README.md)
