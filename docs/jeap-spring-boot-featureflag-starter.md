# Feature flag starter

`jeap-spring-boot-featureflag-starter` provides feature flags (feature toggles) via
[Togglz](https://www.togglz.org/), so features can be turned on or off per stage independently of a
deployment. The point of a feature flag is to **separate deployment from release**: a feature can be
merged and deployed while still switched off, then enabled later — for example once dependent systems
are ready, users have been trained, or a regulation takes effect. This keeps a trunk-based Git workflow
free of long-lived feature branches and "merge hell".

A feature flag is essentially a boolean queried at certain points in the application. It only adds
value if its state can change **at runtime without redeploying**. This starter makes that possible by
wrapping the Togglz state repository in Spring Cloud's `@RefreshScope`, so flag changes delivered
through a config provider (RHOS ConfigMaps, AWS AppConfig, …) take effect on a Spring Cloud config
refresh without a restart. Micrometer metrics are exported per flag.

## How it works

- **`FeatureFlagsConfig`** (`@AutoConfiguration(before = TogglzAutoConfiguration.class)`) defines a
  `@RefreshScope` `stateRepository` bean. It reproduces Togglz's own state-repository selection logic
  (file-based when `togglz.features-file` is set, otherwise in-memory from `togglz.features`, optionally
  wrapped in a `CachingStateRepository`) but adds `@RefreshScope` so the repository is rebuilt on a
  config refresh.
- **`FeatureFlagsMetricsConfig`** (`@ConditionalOnClass(MeterRegistry.class)`) registers a Micrometer
  `feature_flag` `MultiGauge`, one row per feature, value `1` (enabled) or `0` (disabled), tagged with
  `name` (the feature) and `client` (the `spring.application.name`).
- **`TogglzConfigurationEnvPostProcessor`** sets `togglz.web.register-feature-interceptor=false`
  (the servlet/web Togglz pieces are excluded from this starter).

## Configuration

Flags are defined with the standard Togglz properties — inline under `togglz.features.*` or via a
`togglz.features-file`:

```yaml
togglz:
  features:
    MY_FIRST_FEATURE_FLAG:
      enabled: false
    MY_SECOND_FEATURE_FLAG:
      enabled: true
```

Because the state repository is refresh-scoped, changing these values in the config source and
triggering a Spring Cloud config refresh applies the new states without restarting the service.

## Usage in application code

Declare an enum implementing `org.togglz.core.Feature` so flags are type-safe, then query the feature
manager:

```java
public enum FeatureFlags implements org.togglz.core.Feature {

    MICROSERVICE_A_JEAP_1234_DECLARATION_CALCULATION;

    public boolean isActive() {
        return FeatureContext.getFeatureManager().isActive(this);
    }
}
```

```java
if (FeatureFlags.MICROSERVICE_A_JEAP_1234_DECLARATION_CALCULATION.isActive()) {
    // new behaviour
} else {
    // old behaviour
}
```

For swapping whole implementations, Togglz's `FeatureProxyFactoryBean` can front two beans (active /
inactive) behind a single interface. Togglz **activation strategies** can further restrict an enabled
flag to specific users, client IPs, or time windows.

## Notes / pitfalls

- The Togglz servlet/web and Thymeleaf parts are excluded; this starter is for programmatic flag
  evaluation, not the bundled Togglz admin console.
- Runtime flips require a working Spring Cloud config refresh wiring — without a refresh trigger,
  changes are only picked up on restart.
- Metrics require a `MeterRegistry` on the classpath (e.g. via the monitoring starter).

## Related

- [jeap-spring-boot-monitoring-starter](jeap-spring-boot-monitoring-starter.md)
- [Configuration property reference](configuration.md)
- [jeap-spring-boot-starters](../README.md)
