## jEAP Spring Boot Starters
jEAP Spring Boot Starters is a collection of Spring Boot starters to use when developing a Spring Boot application
based on jEAP. The starters provide a set of default configurations and dependencies that are commonly used in jEAP
applications.

The starters include the following projects
* __jeap-spring-boot-application-starter__: Frontend route handling, DB pooling defaults. Includes the 
  jeap-spring-boot-logging-starter and the jeap-spring-boot-cloud-autoconfig-starter.
* __jeap-spring-boot-cloud-autoconfig-starter__: Auto-activate "cloud" profile on CloudFoundry
* __jeap-spring-boot-config-starter__: Spring configuration server client
* __jeap-spring-boot-featureflag-starter__: Feature flag support based on configuration properties and togglz
* __jeap-spring-boot-logging-starter__: Formats logs to include tracing information and use a structured json format
* __jeap-spring-boot-monitoring-starter__: Monitoring using prometheus / micrometer
* __jeap-spring-boot-object-storage-starter__: Configuration of S3 client
* __jeap-spring-boot-postgresql-aws-starter__: PostgreSQL configuration for AWS RDS, supports different cluster types
* __jeap-spring-boot-security-starter__: Secure HTTP/REST APIs using OAuth 2 and provides configs for OAuth2 clients
* __jeap-spring-boot-swagger-starter__: Generates OpenAPI specs for Spring controllers and provides Swagger UI
* __jeap-spring-boot-vault-starter__: Secrets Management with Vault
* __jeap-spring-boot-web-config-starter__: HTTP headers for caching and frontend security headers

## Changes

This library is versioned using [Semantic Versioning](http://semver.org/) and all changes are documented in
[CHANGELOG.md](./CHANGELOG.md) following the format defined in [Keep a Changelog](http://keepachangelog.com/).

## Note

This repository is part the open source distribution of jEAP. See [github.com/jeap-admin-ch/jeap](https://github.com/jeap-admin-ch/jeap)
for more information.

## Attributions

This project includes code from the following open-source projects:

1. **[Spring Framework]**  
   Link: [https://https://github.com/spring-projects](https://github.com/spring-projects)  
   License: Apache 2.0
   Included Code: jEAP is based on the Spring Framework and Spring Boot. Small code snippets from the Spring Framework
   are included in this project, namely in the jeap-spring-boot-config-starter.            
   Changes: Minor modifications to fit project requirements.

## License

This repository is Open Source Software licensed under the [Apache License 2.0](./LICENSE).
