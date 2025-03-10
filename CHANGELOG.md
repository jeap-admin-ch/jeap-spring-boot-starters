# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [17.22.0] - 2025-03-10

### Changed

- Configure proxy to work around the issue https://github.com/aws/aws-sdk-java-v2/issues/4728 which is coming with the aws sdk update

## [17.21.0] - 2025-03-06

### Changed

- Update parent from 5.5.5 to 5.6.0
- Update aws-advanced-jdbc-wrapper from 2.5.0 to 2.5.4

## [17.20.0] - 2025-03-05

### Changed

- Add common-certs as an optional default AppConfig location (AppId) to load in jeap-spring-boot-aws-config-starter

## [17.19.0] - 2025-03-05

### Changed

- Update parent from 5.5.1 to 5.5.5

## [17.18.0] - 2025-02-27

### Changed

- Add new rest endpoint 'current-user' to retrieve the information associated with the authenticated user from the current access token

## [17.17.0] - 2025-02-10

### Changed

- Update parent from 5.5.0 to 5.5.1

## [17.16.0] - 2025-02-07

### Changed

- Update jeap-internal-spring-boot-parent to 5.5.0 (spring boot 3.4.2)
- Publish to maven central

## [17.15.1] - 2025-01-17

### Changed

- Update spring-security-rsa from 1.0.9.RELEASE to 1.1.5

## [17.15.0] - 2024-12-31

### Changed

- Update springdoc to 2.7.0

## [17.14.0] - 2024-12-31

### Changed

- Update parent from 5.4.0 to 5.4.1

## [17.13.0] - 2024-12-18

### Changed

- upgrade to spring boot 3.4.0

## [17.12.1] - 2024-12-17

### Added

- credential scan (trufflehog)

## [17.12.0] - 2024-12-09

### Changed

- Configure trivy scan for all branches

## [17.11.0] - 2024-12-06

### Changed

- Update parent from 5.2.5 to 5.3.0 
- Prepare repository for Open Source distribution

## [17.10.1] - 2024-12-03

### Changed

- Fixed a bug in jeap-spring-boot-tx starter that didn't update the current thread state when getting a transaction fails

## [17.10.0] - 2024-11-21

### Changed

- Enhanced RestRequestLogger to include protocol information (HTTP Version) in log entries

## [17.9.2] - 2024-11-18

### Changed

- Fixing a wrong value for header 'Strict-Transport-Security'

## [17.9.1] - 2024-11-13

### Changed

- Prepare repository for Open Source distribution

## [17.9.0] - 2024-11-12

### Changed

- In AWS RDS Starter is now the usage of the AWS Advanced JDBC wrapper enabled by default
 
## [17.8.0] - 2024-11-08

### Added

- New metric jeap_rest_endpoint_without_jwt to list all requests without jwt authentication

## [17.7.0] - 2024-11-07

### Changed

- Update parent from 5.2.0 to 5.2.1

## [17.6.1] - 2024-11-07

### Changed

- remove FlywayConfigurationCustomizer bean in order to enable other callback beans

## [17.6.0] - 2024-11-06

### Changed

- The JeapOAuth2RestClient/WebclientBuilderFactory methods now check the provided client registration ids and throw an
  IllegalArgumentException if there is no client configured for the given id. This helps to identify configuration errors
  already during application startup.

## [17.5.3] - 2024-11-06

### Added

- org.postgresql dependency to jeap-spring-boot-db-migration-starter

## [17.5.2] - 2024-11-05

### Added

- License definition & license plugins

## [17.5.1] - 2024-11-04

### Changed

- Fixed wrong property value

## [17.5.0] - 2024-11-04

### Changed

- Improved Postgres AWS RDS starter to optionally use the AWS Advanced JDBC Driver

## [17.4.0] - 2024-10-31

### Changed

- Update parent from 5.1.0 to 5.1.1

## [17.3.0] - 2024-09-20

### Changed

- Update parent from 5.0.0 to 5.1.0

## [17.2.4] - 2024-09-18

### Changed

- Setting "try it out" to true for Swagger UI

## [17.2.3] - 2024-09-13

### Changed

- Fix missing '$' in database name property for read replicas

## [17.2.2] - 2024-09-13

### Changed

- Fix bean post processor eagerly initializing monitoring beans before the application context is fully initialized

## [17.2.1] - 2024-09-12

### Changed

- DB transaction metric counts read replica transactions separately from read-write transactions

## [17.2.0] - 2024-09-11

### Changed

- AWS RDS read replica routing is now activated explicitly using an annotation (@TransactionalReadReplica)

## [17.1.0] - 2024-09-06

### Changed

- removed configurable plaintext metrics format only because it is no longer necessary

## [17.0.0] - 2024-09-06

### Changed

- Update parent from 4.11.1 to 5.0.0 (java 21)

## [16.4.0] - 2024-09-04

### Changed

- postgresql-aws: add new metric to count read-only and read-write transactions 
- postgresql-aws: configure default hikari settings
- postgresql-aws: set replica db-name to database-name

## [16.3.0] - 2024-08-22

### Changed

- Fix dependency version metrics for image-based jEAP apps

## [16.2.0] - 2024-08-21

### Changed

- Update parent from 4.10.0 to 4.11.1

## [16.1.4] - 2024-08-19

### Changed

- Adjusted the monitoring-starter integration tests to a configuration that sets a specific management port and a specific management context-path.

## [16.1.3] - 2024-08-13

### Changed
- The start up migrate mode is enabled per default, if the Plattform is not Kubernetes for jeap-spring-boot-db-migration-starter. 

## [16.1.2] - 2024-08-02

### Changed
- jeap-spring-boot-vault-starter should be disabled, if the application is the init migration job.

## [16.1.1] - 2024-07-31

### Changed
- jeap-spring-boot-vault-starter: Extend RHOS/Kubernetes authentication for the use of vault core

## [16.1.0] - 2024-07-26

### Changed
- jeap-spring-boot-vault-starter: Extend RHOS/Kubernetes authentication for Vault

### Added
- jeap-spring-boot-vault-starter: Platform Dependent Config Loggers 

## [16.0.0] - 2024-07-15

### Changed

- Added jeap-spring-boot-object-storage-starter.

## [15.4.0] - 2024-07-16

### Added

- jeap-spring-boot-vault-starter: Added RHOS/Kubernetes authentication for Vault

## [15.3.0] - 2024-07-15

### Changed

- Upgrading jeap-internal-spring-boot-parent to 4.10.0, containing the upgrade to Spring boot 3.3.1
 
## [15.2.1] - 2024-07-12

### Changed

- Shut down the init container immediately after the migration without loading any further context. Prevents connection to other systems.

## [15.2.0] - 2024-06-27

### Changed

- Update springdoc to 2.5.0

## [15.1.0] - 2024-06-25

### Changed

- Add support for rhos log format.

## [15.0.0] - 2024-06-19

### Changed

- Added jeap database migration starter.

## [14.15.1] - 2024-06-10

### Changed

- Ignore exceptions due to HikariConfig modification attempts, usually triggered by AppConfig config updates.

## [14.15.0] - 2024-06-05

### Changed

- Configurable plaintext metrics format only that the actual prometheus instance on RHOS is able to understand it

## [14.14.0] - 2024-06-03

### Changed

- Simplification of jeap postgresql aws starter properties
- Now the replica database must be explicitly activated with 'jeap.datasource.replica.enabled=true'

## [14.13.0] - 2024-05-17

### Changed

- Add common-platform to the default list of configurations read in AWS AppConfig

## [14.12.0] - 2024-05-08

### Changed

- Log aws appConfig startup exceptions to system out (no logger is available before the startup)

## [14.11.0] - 2024-05-03

### Changed

- Update parent from 4.8.2 to 4.8.3

## [14.10.0] - 2024-04-09

### Changed

- Logging Starter: Add taskDefinitionVersion for AWS-Cloudwatch logs

## [14.9.0] - 2024-03-28

### Changed

- Enable traceId propagation for reactor by default
- Remove sleuth old configuration (spring boot 2)

## [14.8.0] - 2024-03-27

### Changed

- Update parent from 4.8.0 to 4.8.2

## [14.7.0] - 2024-03-04

- Upgraded to jeap internal parent 4.8.0 (spring boot 3.2.3)

## [14.6.1] - 2024-02-26

- Fixed JeapOAuth2IntegrationTestClientConfiguration also requiring webflux for RestClient only test configurations.

## [14.6.0] - 2024-02-05

- Add FrontendRouteRedirectExceptionHandler for server-side SPA routing

## [14.5.0] - 2024-02-01

- Adding a new starter jeap-spring-boot-tls-starter that will automatically enable TLS on the Spring Boot embedded webserver
  using a self-signed certificate created on the fly at start-up.

## [14.4.0] - 2024-01-25

- Upgraded jeap-internal-spring-boot-parent from 4.4.1 to 4.5.0 (spring boot 3.2.2)

## [14.3.0] - 2024-01-23

- Split configuration of postgresql-aws-starter to enable transaction checking independently of environment
- Postgresql-aws-starter DataSource configuration is disabled by default and must be manually enabled via jeap.postgresql.aws.enabled=true

## [14.2.3] - 2024-01-23

- Upgraded jeap-internal-spring-boot-parent from 4.4.0 to 4.4.1
- add OAuth2RestClientConfiguration to DisableJeapOAuth2ClientConfiguration

## [14.2.2] - 2024-01-23

- define new property jeap.config.client.fail-fast in order to configure the spring property spring.cloud.config.fail-fast

## [14.2.1] - 2024-01-19

- Fixed NPE on calls to /api-docs/Actuator

## [14.2.0] - 2024-01-18

- Support for the OAuth2 client credentials flow using the RestClient
- Add APPLICATION_NAME_HEADER (JEAP-APPLICATION-NAME) as default header in RestClientBuilder

## [14.1.0] - 2024-01-16

- Upgraded jeap-internal-spring-boot-parent from 4.3.2 to 4.4.0

## [14.0.0] - 2024-01-08

- Removed the automatic activation of the spring cloud bootstrap context via the dependency spring-cloud-starter-bootstrap.
- The config-starter and vault-starter now support configuration and secret imports via the spring.config.import property.
- Upgraded jeap-internal-spring-boot-parent from 4.3.0 to 4.3.2

## [13.11.0] - 2023-12-13

- Upgraded jeap-internal-spring-boot-parent from 4.2.0 to 4.3.0 (spring boot 3.2)

## [13.10.6] - 2023-12-12

- Setting https as protocol for Swagger Server Base URL when not in localhost. This is primarily used on architectures
  where the microservice is behind a load balancer or reverse proxy enforcing https

## [13.10.5] - 2023-12-08

- Add DefaultCredentialsProvider bean to JeapAWSAppConfigAutoConfig to avoid applying the default AWS credentials
  provider autoconfiguration from io.awspring.cloud, which requires a region to be set under certain conditions.

## [13.10.4] - 2023-12-05

- Avoid early instantiation warning due to PlatformTransactionManagerBeanPostProcessor not being a static bean
  definition

## [13.10.3] - 2023-12-04

- Use forward slash in app config locations to avoid the need for escaping backslashes
- Add missing apache HTTP client exclusion for the postgres starter

## [13.10.2] - 2023-11-29

- Setting default region to eu-central-2 (Zürich) in PostgreSQL AWS Starter

## [13.10.1] - 2023-10-27

- Remove old pinned version of nimbus-jose-jwt

## [13.10.0] - 2023-10-19

- Endpoints under /<consumertype>-api are not cached anymore

## [13.9.1] - 2023-10-19

- jeap-spring-boot-postgresql-aws-starter now uses the set password if given. This improves local development and testing.

## [13.9.0] - 2023-10-17

- jeap-spring-boot-postgresql-aws-starter now checks whether read-write transactions are nested in top level read-only transactions in all environments, including local. 

## [13.8.1] - 2023-10-12

- Adding option to trust all certificates when connecting to AWS AppConfig for development purposes.

## [13.8.0] - 2023-10-11

- Add spring-cloud-aws-starter-secrets-manager dependency in jeap-spring-boot-config-aws-starter

## [13.7.1] - 2023-10-10

- Fixed caching for endpoints for /ui-api under some circumstances

## [13.7.0] - 2023-10-09

- Upgraded jeap-internal-spring-boot-parent from 4.0.0 to 4.2.0
- Disabled caching for endpoints under /ui-api

## [13.6.0] - 2023-10-03

- Switched to a different configuration structure in AWS AppConfig by now modelling microservice configurations as applications
  instead of as profiles like before.

## [13.5.1] - 2023-09-29

- Fixed app config refresh not working when bootstrap context used and no specific app config profiles configured.

## [13.5.0] - 2023-09-28

- Extracted the feature flag support from the config-starter project into a new featureflag-starter project.

## [13.4.0] - 2023-09-27

- Added automatic spring context refresh with configuration changes from AWS AppConfig.

## [13.3.1] - 2023-09-25

- Fixed instantiation of dependent beans TrustStoreMetricsInitializer and TrustStoreService

## [13.3.0] - 2023-09-15

- Added AWS AppConfig as PropertySource

## [13.2.1] - 2023-09-14

- Fixed CVE-2023-34035 related problem in jeap-spring-boot-security-starter-test: Factory method 'actuatorSecurityFilterChain'
  threw exception with message: This method cannot decide whether these patterns are Spring MVC patterns or not.

## [13.2.0] - 2023-09-13

- Added support for read-only replicas in jeap-spring-boot-postgresql-aws-starter

## [13.1.3] - 2023-09-07

- Fixed overly permissive actuator security configuration for the actuator role

## [13.1.2] - 2023-09-04

- Added: New AWS AppConfig integration

## [13.1.1] - 2023-09-01

- Fix logback include path

## [13.1.0] - 2023-09-01

- Add support for cloudwatch log format, internal refactoring of the logback configuration

## [13.0.2] - 2023-08-29

- Fixed: Factory method 'actuatorSecurityFilterChain' threw exception with message: This method cannot decide whether
  these patterns are Spring MVC patterns or not.

## [13.0.1] - 2023-08-21

- Fixed: Log statements contained full log configuration context instead of only spring boot app name

## [13.0.0] - 2023-08-16

- Upgrading to spring boot 3.1

## [12.7.3] - 2023-07-19

- Fix: jeap_spring_app metric is now active by default

## [12.7.2] - 2023-07-17

- Automatic upgrade of jeap-spring-boot-parent when released

## [12.7.1] - 2023-06-23

- Upgrade internal parent to 3.5.0

## [12.7.0] - 2023-06-22

- Configured tracing to support w3c propagation in addition to the currently only b3 propagation
  and configured the tracing to match the tracing defaults in spring boot 3.
- Fixed tracing headers missing in the console and file log appenders. 

## [12.6.2] - 2023-06-12

- Improve property file format for health probes
- Remove JeapTimed annotation 

## [12.6.1] - 2023-05-11

- HealthMetricsConfig is now public (instead of package private). This way it can be excluded in case an application
  doesn't want HealthIndicators being executed each time /actuator/prometheus is called

## [12.6.0] - 2023-05-10

- Added support for customizing the method security expression handler created by jEAP.
- Added support for configuring a list of authorization servers to trust instead of just being able to configure one
  server
  for the user and system contexts and one server for the b2b context.

## [12.5.2] - 2023-04-25

- Set max header size to 64 KB

## [12.5.1] - 2023-04-20

- Upgrade internal parent to 3.4.1 (spring boot 2.7.11)

## [12.5.0] - 2023-04-18

- Add `@JeapTimed` annotation to enable timed metrics with default percentiles

## [12.4.0] - 2023-04-13

- Added support for new token claim 'admin_dir_uid'. Convenience methods added to relevant classes such
  as `JeapAuthenticationToken.getAdminDirUID()`
  and `JwsBuilder.withAdminDirUID()`

## [12.3.2] - 2023-03-22

- Update SwaggerProperties with openIdConnectUrl property

## [12.3.1] - 2023-03-17

- Fix logback metrics counters stopping to be incremented after a context refresh event (in config clients) 

## [12.3.0] - 2023-03-06

- Add spring-boot-starter-validation as dependency in jeap-spring-boot-vault-starter

## [12.2.0] - 2023-02-27

- Downgrading springdoc-openapi from 1.6.6 to 1.6.5 because of issue with password for client credentials login
- Add cluster information to logrelay log statements

## [12.1.0] - 2023-02-20

- Upgrading to Java 17
- Upgrading dependency versions (e.g. jeap-internal-spring-boot-parent to 3.4.0) 

## [12.0.2] - 2023-02-03

- Reduce log volume on level info

## [12.0.1] - 2023-02-01

- Fixed missing test scope in application starter for spring boot test starter

## [12.0.0] - 2023-01-20

- Breaking: Update SwaggerOAuthConfiguration with OPENIDCONNECT instead OAUTH2
  - these 2 properties are deleted: `jeap.swagger.oauth.tokenUrl` / `jeap.swagger.oauth.authorizationUrl`
  - If required, use the new property `jeap.swagger.oauth.openIdConnectUrl` to configure swagger

## [11.5.2] - 2023-01-13

- Fixed default constructor on JeapAuthenticationConverter missing, re-added the constructor for backward compatibility.

## [11.5.1] - 2023-01-11

- Bugfix: jeap-spring-boot-config-starter: Avoid NullPointer when SecurityProtocol is not set
- Deprecate `createForClientId` in favor of `createForClientRegistryId` (rename method).

## [11.5.0] - 2022-12-20

- Removed deprecated jeap authorization and added simple role authorization.

## [11.4.0] - 2022-12-14

- Add support for custom authorities resolver

## [11.3.2] - 2022-12-08

- Stop forcing JeapOAuth2IntegrationTestResourceConfiguration based WebMVC integration tests having to provide reactor as test dependency. 

## [11.3.1] - 2022-12-07

- The 'ctx' claim may now be missing from an access token if the claim will be mapped by a claim set converter.

## [11.3.0] - 2022-12-06

- Added an option to configure a claim set converter for the authorization server and b2b gateway in the OAuth2 resource server configuration. 

## [11.2.2] - 2022-11-18

- Upgraded logstash-logback-encoder to latest version and forcing logstash to use the spring boot managed jackson databind version.

## [11.2.1] - 2022-11-16

- Fixes vault configuration property initialization

## [11.2.0] - 2022-11-10

- Adding support for the new additional jEAP messaging consumer kafka brokers config option to the jEAP config starter.
- jeap-spring-boot-web-config-starter: Avoid adding security headers for actuator endpoints

## [11.1.0] - 2022-10-21

- Configure tomcat/spring to use relative redirects by default

## [11.0.2] - 2022-10-14

- jeap-spring-boot-web-config-starter: configure '/' for no caching

## [11.0.1] - 2022-10-12

- Fixed swagger starter Actuator API group preconfiguration does not respect actuator base path configuration.

## [11.0.0] - 2022-10-04

- Added a LogFilter in jeap-spring-boot-logging-config-starter, which filter out all messages containing 'Found no committed offset for partition'
- Upgraded to jeap-internal-spring-boot-parent 3.3.0 (spring boot 2.7)
- Breaking: Spring security configurations migrated to SecurityFilterChain beans instead of deprecated WebSecurityConfigurerAdapter classes.

## [10.4.0] - 2022-09-21

- Add jeap-spring-boot-web-config-starter, supplying default HTTP headers for security and caching

## [10.3.1] - 2022-09-02

- Add DbPoolingDefaultsEnvPostProcessor to set sensible default hikari connection pool size (0 - 4 connections, same as
  the CF java buildpack spring boot auto reconfiguration used)

## [10.3.0] - 2022-08-15

- Add jeap-spring-boot-cloud-autoconfig-starter as a replacement for the
  deprecated https://github.com/cloudfoundry/java-buildpack-auto-reconfiguration

## [10.2.1] - 2022-08-11

- Fix config client not being able to use properties from vault as the jeap config server authentication credentials in
  some projects.

## [10.2.0] - 2022-06-28

- Upgrade jeap-internal-spring-boot-parent to 2.4.6 (javadoc disabled per default)
- Enable the javadoc
- Added the option to use properties from vault as the jeap config server authentication credentials in jeap config
  client.
  The usage of the preconfigured Cloud Foundry User Provided Service "config" to store the authentication credentials is
  now deprecated.

## [10.1.0] - 2022-05-25
- Both vault and jeap-vault profiles suppress "Invalid cookie header" warnings

## [10.0.2] - 2022-05-30
- Fixed configuration refresh in config client not taking account of removed config server property sources.

## [10.0.1] - 2022-05-20
- Exposes any property that starts with info.* by default in actuator info (`management.info.env.enabled`)

## [10.0.0] - 2022-05-18
- All SemanticRoleRepository methods (which are also accessible in the SPEL expressions of Pre/PostAuthorize declarations)
  now consider only roles applying to the system the SemanticRoleRepository has been configured with. Until now this has not
  been the case for the methods getAllRoles, getAllRolesForPartner, getAllRolesForAllPartners, getUserRoles, getBusinessPartnerRoles.
  This fits better with developer expectations and thus should reduce bugs caused by misunderstandings of the SemanticRoleRepository API.

## [9.9.1] - 2022-05-17

- ConfigClient: Fail Fast when Server is not running

## [9.9.0] - 2022-05-09

- New Prometheus Metric: feature_flag

## [9.8.2] - 2022-04-04

- Upgrade internal parent from 2.3.2 to 2.3.3 (upgrade internal parent to 2.3.3
  fixing https://spring.io/blog/2022/03/31/spring-framework-rce-early-announcement)

## [9.8.1] - 2022-03-31

- Upgrade internal parent from 2.3.1 to 2.3.2 (pin dependency to spring-cloud-function-context to 3.2.3 due
  to https://tanzu.vmware.com/security/cve-2022-22963)

## [9.8.0] - 2022-03-29

- Move sleuth dependency to jeap-spring-boot-logging-starter (where it actually belongs)
  - Sleuth issue on spring context initialization is fixed by
    now (https://github.com/spring-cloud/spring-cloud-sleuth/issues/2106)

## [9.7.0] - 2022-03-23

- Improve retry and keepalive for syslog connections

## [9.6.4] - 2022-03-22

- Do not register as rest relation the requests without uri pattern

## [9.6.3] - 2022-03-21

- Use name claim instead of sub claim when logging user access with UserAccessLoggingRequestFilter

## [9.6.2] - 2022-03-15

- Add `@WithJeapAuthenticationToken` annotation for easier integration testing with MockMvc

## [9.6.1] - 2022-03-11

- Suppress "Invalid cookie header" warnings caused by vault web app firewall

## [9.6.0] - 2022-03-09

- Upgrade internal parent to 2.3.0 (spring boot 2.6.4, spring cloud 2021.0.1 patch release update)

## [9.5.0] - 2022-02-10

- Add UserAccessLoggingRequestFilter

## [9.4.1] - 2022-02-08

- Move sleuth dependency to jeap-monitoring-starter (where it belongs)
  - Seems to fix https://github.com/spring-cloud/spring-cloud-sleuth/issues/2106 for some apps

## [9.4.0] - 2022-01-28

- Expose trusted certificate validity metrics

## [9.3.0] - 2022-01-21

- Swagger Starter: Update springdoc-openapi to 1.6.4

## [9.2.1] - 2022-01-05

- Config Client: Checks if the Topic for spring.cloud.bus.destination is set

## [9.2.0] - 2021-12-22

- Upgrade parent to version with spring boot 2.6.2 / spring cloud 2021.0.0

## [9.1.3] - 2021-12-10

- FeatureFlag enhancement

## [9.1.2] - 2021-11-19

- Java 17 readiness (internal parent 2.1.5 with lombok update), updated tests

## [9.1.1] - 2021-11-12

- Update jeap-internal-spring-boot-parent version

## [9.1.0] - 2021-11-08

### Added

- Added options to log authentication failures and access denied errors if configured. 
- Support for providing own implementations of AccessDeniedHandler and AuthenticationEntryPoint to be used by the 
OAuth2 resource server configuration.
- Improved OAuth2 authentication exception handling 

## [9.0.2] - 2021-10-28

### Changed

- Set specific syslog hostname prefix for correct JSON escaping in splunk
- Add metric for spring boot app name
- Add spring boot app name to REST tracing metrics

## [9.0.1] - 2021-10-26

### Changed

- Update jeap-internal-spring-boot-parent version

## [9.0.0] - 2021-10-26

## Fixed

- JEAP-2502: Removed whitespaces in "name" attribute of SecuritySchemes in SwaggerOauthConfiguration. This makes it
  compliant with OpenAPI Spec v3. For further information see jeap blog post from 27.10.2021.
    - "OIDC Enduser" became "OIDC_Enduser"
    - "OIDC System" became "OIDC_System"

## [8.5.1] - 2021-10-14

### Changed

- Exclude unused togglz dependencies

## [8.5.0] - 2021-10-04

### Added

- Added separate jEAP config client configuration properties for spring cloud bus kafka, which get automatically
  configured if jEAP messaging kafka configuration properties are already configured.

## [8.4.0] - 2021-09-21

### Added

- jeap-spring-boot-config-starter: Added Togglz for FeatureFlags

## [8.3.0] - 2021-09-17

### Added

- Adding support for authorization checks just against a given system and operation, allowing for programmatic
  validation of the resource

## [8.2.0] - 2021-08-23

### Added

- Profile for log tee to file-system for tailing logs in test environments

## [8.1.3] - 2021-07-14

### Changed

- Reduce log level for kafka configs to ERROR

## [8.1.2] - 2021-07-06

### Changed

- Reconnect attempt on syslog transmit error

## [8.1.1] - 2021-06-25

### Changed

- Immediate single retry on syslog transmit error

## [8.1.0] - 2021-06-24

### Added

- Support for distributed logging to syslog

## [8.0.2] - 2021-06-22

### Changed

- Upgraded to jeap-internal-spring-boot-parent 2.0.2 (includes upgrade to Spring Boot 2.5.1)

## [8.0.1] - 2021-06-03

### Changed

- Use newer nimbus-jose-jwt version which accepts duplicate JSON keys in the JWT

## [8.0.0] - 2021-06-01

### Changed

- Upgraded to jeap-internal-spring-boot-parent 2.0.0 (most notably, upgrading to Spring Boot 2.5.0)

## [7.6.1] - 2021-05-06

### Fixed

- The jEAP jUnit test extension @WithAuthentication now also works on @ParameterizedTest tests.

## [7.6.0] - 2021-05-05

### Added

- Readiness health probe

## [7.5.0] - 2021-04-16

### Added

- Vault starter

## [7.4.1] - 2021-03-24

### Changed

- Default REST request logging level changed from DEBUG to TRACE
- Default REST response logging level changed from INFO to DEBUG
- Add flag jeap.rest.tracing.fullResponseDetailsInMessage to allow for full response detail logging in log message

## [7.4.0] - 2021-03-02

### Added

- jeap-spring-boot-config-starter now exposes the active configuration version under the actuator info endpoint.

## [7.3.2] - 2021-02-23

### Fixed

- Log request URI instead of URI pattern in REST logger
- Remove double quotes on actuator filter pattern in REST request logger
- Fix bean dependency in RestRequestLogger for non-webapps

## [7.3.1] - 2021-02-19

### Changed

- Filter actuator requests by default in REST request/response logging to avoid log spamming

## [7.3.0] - 2021-02-09

### Added

- Produces REST request tracing monitoring metrics
- Internal: REST tracing split into tracing and logging
- Renamed configuration property prefix jeap.rest.log -> jeap.rest.tracing (breaking change, but not used in any client
  apps)

## [7.2.1] - 2021-02-05

### Changed

- Added new method getPreferredUsername in JeapAuthenticationToken to get the claim preferred_username.

## [7.2.0] - 2021-01-22

### Fixed

- Respect tenant wildcards in checked role

## [7.1.2] - 2021-01-21

### Fixed

- Simplified dependency version metrics, removed version parsing

## [7.1.1] - 2021-01-19

### Fixed

- Bugfixes for dependency version metrics
- Fixed missing deactivation of CSRF in the PermitAllWebSecurityConfiguration and lowered the priority of the
  PermitAllWebSecurityConfiguration to allow for overriding it if needed.

## [7.1.0] - 2020-12-15

### Changed

- Added metrics for dependency versions

## [7.0.0] - 2020-11-25

### Changed

- Renamed semantic application role part 'function' to 'operation'.

## [6.1.0] - 2020-11-14

### Added

- Additional functionality for semantic application roles and the corresponding test support.

### Fixed

- Generate Javadoc and Sources artefacts

## [6.0.0] - 2020-11-17

### Added

- Semantic Application Roles

### Changed

- Class JeapAuthenticationToken and other moved from ch.admin.bit.jeap.security.resource.authentication to
  ch.admin.bit.jeap.security.resource.token

### Removed

- Removed MinIO. New projects should no longer use MinIO. Use StorageGRID instead.

## [5.1.1] - 2020-11-11

### Fixed

- Fixed "OAuth2 client requesting superfluous 'offline_access' scope (and others) when no scope is configured for a
  client in the Spring Boot client registration configuration."

## [5.1.0] - 2020-11-10

### Changed

- Migrate Minio from major release 6 => 7

## [5.0.0] - 2020-10-28

### Changed

- The jeap-spring-boot-config-server-starter has been removed. It has been replaced by the separate jeap-config-server
  project. The jeap-spring-boot-config-starter has been adapted to work with the new jeap-config-server, i.e. it no
  longer includes Spring Cloud Bus to receive configuration change notifications.

## [4.4.2] - 2020-09-30

### Fixed

- Fixed "Additional permitted monitoring endpoints that are WebEndpoints would not get enabled".
- Fix "Logfile actuator unable to find log file configured by the jeap-spring-boot-logging-starter for the profile '
  cloud'". At the same time reducing the rolling file logger configured by the starter to only two files but of
  increased size.

## [4.4.1] - 2020-09-23

### Fixed

- Fixed 'log level cannot be set with default actuator configuration for Spring Boot Admin' in
  jeap-spring-boot-monitoring-starter.
- Added automatic enabling of additional permitted monitoring endpoints in jeap-spring-boot-monitoring-starter.
- Removed jolokia endpoints from admin endpoints, stopped exposing actuators over JMX.
- Restricted actuator endpoints to GET requests only with exception of the loggers endpoint which additionally supports
  POST requests if 'admin endpoints' are enabled.
- Fixed missing log in spring boot admin

## [4.4.0] - 2020-09-08

### Added

- Add default actuator configuration for Spring Boot Admin. Enable with property
  jeap.monitor.actuator.enable-admin-endpoints.
- Protect actuator endpoints, disable by default except for info & health

### Removed

- Removed unused profile disableprometheussecurity

## [4.3.1] - 2020-08-17

### Fixed

- Do not show actuator when springdoc.show-acturator: false is set

## [4.3.0] - 2020-08-17

### Added

- Added System-Context-Login on Swagger UI

### Fixed

- Fixed some errors in the Minio exception handling.

## [4.2.0] - 2020-08-06

### Changed

- Updated Dependencies

## [4.1.2] - 2020-06-26

### Fixed

- Avoid loading request tracer with dependency to spring-web in non-web-modules

## [4.1.1] - 2020-06-17

### Fixed

- Changed parent to jeap internal parent, excluding jeap dependencies

## [4.1.0] - 2020-06-16

### Added

- OAuth2 resources can now be configured to rely on just a B2B gateway instance for authentication.

### Fixed

- Removed special role parsing for B2B tokens.

## [4.0.2] - 2020-06-10

### Fixed

- Fixed OAuth2 WebClient instances not reusing tokens outside of an HTTP request in the WebFlux stack.
- Upgraded to Spring Boot version 2.2.7 from 2.2.4 (needed for the fix)

## [4.0.1] - 2020-06-04

### Fixed

- Wrong header name
- Potential NPE in logging

## [4.0.0] - 2020-06-04

### Added

- Request-Tracing in Log-Starter

### Changed

- Moved to Java 11, no support for Java 8 any more

## [3.5.1] - 2020-05-28

### Fixed

- Fixed the problem that WebClient instances built with JeapOAuth2WebClientBuilderFactory builders could not be used
  outside of an HTTP request in the WebMvc stack.

## [3.5.0] - 2020-05-15

### Added

- Swagger starter

## [3.4.1] - 2020-04-17

### Fixed

- Wrong scopes in application starter

## [3.4.0] - 2020-04-16

### Added

- Application starter to be used from the parent project

### Changed

- Updated to latest spring version

## [3.3.0] - 2020-03-27

### Added

- Added support for testing OAuth2 protected REST endpoints of Spring Boot applications protected by
  jeap-spring-boot-security-starter.

## [3.2.0] - 2020-03-10

### Added

- Added some support for convenient testing or mocking of authorization checks and aspects in the new module
  jeap-spring-boot-security-starter-test.
- Added convenient accessors for certain JWT token claims (name, given_name, family_name, locale, jeap authentication
  context).

### Fixed

- Fixed a bug in the CSRF configuration for the WebMvc stack.

## [3.1.2] - 2020-03-03

### Added

- Publish status of the health endpoint as metric 'health' to prometheus

### Changed

- Allow public construction of JeapAuthenticationToken.

## [3.1.1] - 2020-02-13

### Fixed

- Fixed a bug introduced with version 3.1.0 that a WebMvc application would not start if it was an OAuth2 resource but
  not an OAuth2 client and did not depend on WebClient.

## [3.1.0] - 2020-02-13

### Added

- Support for creating WebClient instances that use the access token of the current incoming request to authenticate
  outgoing requests.

## [3.0.0] - 2020-02-03

### Added

- Support for validating B2B gateway tokens.
- Activating a 'deny-all' security configuration in security starter if OAuth2 security is not activated.

### Changed

- Changed configuration of the OAuth2 resource in order to also support tokens from the B2B gateway and to make the
  configuration simpler.
- Name and semantics of the jEAP authorization bean method hasBusinesspartnerRole(..) changed to
  hasRoleForBusinessPartner(..)
  which bases authorization not only on business partner roles but also on user roles assuming that user roles apply to
  all partners.

## [2.0.0]

### Added

- Support for OAuth2 protected Spring WebFlux resources
- Support for the OAuth2 client credentials flow using the reactive WebClient

### Changed

- Upgraded Spring Security version to 2.2.2-RELEASE
- Replaced OAuth2 implementations of the Spring Security OAuth2 project with the (newer) standard Spring Security
  implementations.
- Actuator path is no longer excluded from the authentication check

### Removed

- Removed the OAuthRestTemplate provided previously by the Spring Security OAuth2 project
- Moved parent to seperate repository

## [1.2.1] - 2019-12-18

### Changed

- Fixed multiple small issues with Java 11 parent

## [1.2.0] - 2019-12-11

### Added

- A new parent pom that can be used either as parent or as dependency BOM

## [1.1.0] - 2019-11-07

### Added

- Logging: RollingFileAppender for spring-boot-admin if SpringBootAdm is enabled

## [1.0.0] - 2019-11-05

### Added

- Logging, monitoring and config starters
- PoC of a security starter using keycloak
- A MinIO-Configuration starter that is ready to use but needs to be updated

### Changed

Nothing

### Removed

Nothing
