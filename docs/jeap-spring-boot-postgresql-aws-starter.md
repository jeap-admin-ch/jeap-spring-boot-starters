# PostgreSQL AWS starter

`jeap-spring-boot-postgresql-aws-starter` simplifies connecting a jEAP Spring Boot service to an
**AWS RDS / Aurora PostgreSQL** database. RDS provides fully managed relational databases on AWS;
Aurora follows a single-primary replication model where one writer instance handles writes and up to
15 read-only replicas can offload read traffic. The starter wires up IAM authentication, SSL,
sensible HikariCP defaults and — for multi-instance clusters — read-replica transaction routing,
so services do not have to assemble this configuration themselves.

When this starter is active, `spring.datasource.*` is **ignored**; the datasource is configured under
the `jeap.datasource.*` prefix instead.

## Enabling the starter

The starter is disabled by default and is switched on with a property (the Maven dependency version
is managed by the jEAP Spring Boot parent):

```yaml
jeap:
  postgresql:
    aws:
      enabled: true
```

## How it works

`JeapPostgreSQLAWSDataSourceAutoConfig` runs *before* Spring's `DataSourceAutoConfiguration` and
builds a HikariCP datasource. Two ingredients are central:

- **AWS Advanced JDBC Wrapper** — `HikariDataSourceFactory` wraps the JDBC URL as
  `jdbc:aws-wrapper:postgresql://…` and uses `AwsWrapperDataSource` over `PGSimpleDataSource`. The
  wrapper understands the RDS cluster topology and provides IAM authentication and faster failover,
  so an RDS Proxy is no longer required. The default wrapper plugins are
  `auroraConnectionTracker, failover, efm2, iam`. (The wrapper is the default and the only supported
  mode; the pre-wrapper logic has been removed.)
- **IAM authentication** — instead of a password, the application authenticates with a short-lived
  IAM token generated automatically by the wrapper's `iam` plugin. Because the default RDS IAM token
  lifetime is 15 minutes, the starter sets HikariCP `max-lifetime=840000` (14 min) so connections
  rotate *before* their token expires. The AWS credentials used to mint the token are read from the
  standard environment (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION`,
  optionally `AWS_PROFILE`).

For local development with H2, the factory detects the test database (`jdbc:h2:` URL or `org.h2.Driver`)
and builds a plain HikariCP datasource without the wrapper, since the wrapper is not H2-compatible. A
password is then required (there is no IAM token locally), and the Hikari schema must be set to the H2
default (e.g. `PUBLIC`) because the starter otherwise defaults the schema to `data`.

### Inferred URL, username and database name

If `jeap.datasource.url` is unset, the JDBC URL is built from
`jeap.datasource.aws.hostname` / `.port` / `.database-name`, with the SSL query string
`?ssl=true,sslMode=verify-full,sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory` appended.
Unset names are derived from `spring.application.name`, snake-cased: the username defaults to
`{app}_db_rwa` (primary), `{app}_db_ro` (replica), and the database name to `{app}_db`.

## Configuration

```yaml
jeap:
  postgresql:
    aws:
      enabled: true
  datasource:
    url: "jdbc:postgresql://my-cluster.rds.amazonaws.com:5432/my_db?ssl=true,sslMode=verify-full,sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory"
    username: my_db_rwa          # optional; inferred from app name if omitted
    aws:
      region: eu-central-2
```

| Property                                                      | Default        | Description                                                      |
|---------------------------------------------------------------|----------------|------------------------------------------------------------------|
| `jeap.postgresql.aws.enabled`                                 | `false`        | Enable the starter                                               |
| `jeap.datasource.url`                                         | inferred       | Full JDBC URL of the read/write endpoint                         |
| `jeap.datasource.username`                                    | `{app}_db_rwa` | DB username (inferred from `spring.application.name`)            |
| `jeap.datasource.password`                                    | —              | Set only locally; if set, IAM token auth is disabled             |
| `jeap.datasource.aws.region`                                  | `eu-central-2` | AWS region of the database                                       |
| `jeap.datasource.aws.hostname`                                | —              | RDS endpoint host (used to build the URL when `url` unset)       |
| `jeap.datasource.aws.port`                                    | `5432`         | RDS port                                                         |
| `jeap.datasource.aws.database-name`                           | `{app}_db`     | Database name (inferred from app name if omitted)                |
| `jeap.datasource.aws.enable-advanced-jdbc-wrapper`            | `true`         | Enable the AWS Advanced JDBC Wrapper                             |
| `jeap.datasource.aws.wrapper.target-data-source-properties.*` | see below      | Properties passed to the wrapper's target datasource (camelCase) |
| `jeap.datasource.hikari.*`                                    | see below      | HikariCP pool tuning for the primary (writer)                    |

Starter-applied Hikari defaults for the primary pool: `schema=data`, `maximum-pool-size=4`,
`minimum-idle=0`, `keepalive-time=120000`, `pool-name=hikari-cp-rw`, `max-lifetime=840000`. JPA's
`hibernate.default_schema` defaults to `data`. The wrapper target property
`...wrapper.target-data-source-properties.wrapperPlugins` defaults to
`auroraConnectionTracker,failover,efm2,iam`.

## Read replicas

In a multi-instance Aurora cluster, read-only replicas can serve read traffic and relieve the writer.
Replicas are opt-in:

```yaml
jeap:
  datasource:
    replica:
      enabled: true
      url: "jdbc:postgresql://my-cluster-ro.rds.amazonaws.com:5432/my_db?ssl=true,sslMode=verify-full,sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory"
      username: my_db_ro         # optional; inferred as {app}_db_ro
      aws:
        region: eu-central-2
```

When `jeap.datasource.replica.enabled=true`, `RDSReadOnlyReplicaAutoConfiguration` builds a second
HikariCP datasource (`pool-name=hikari-cp-ro`, username defaulting to `{app}_db_ro`) under
`jeap.datasource.replica.*` (with `.aws.*` and `.hikari.*` subtrees mirroring the primary). A
`@Primary` `ReadReplicaAwareTransactionRoutingDataSource` then exposes both as `writer` and `reader`
targets and routes read-only top-level transactions to the replica. The routing primitive and the
`@TransactionalReadReplica` annotation that triggers it are provided by
[`jeap-spring-boot-tx`](jeap-spring-boot-tx.md) — see that page for the routing semantics.

**Eventual consistency:** the replica page cache is updated asynchronously, so a replica may return
slightly stale data (on the PROD application platform the `AuroraReplicaLag` is roughly 15 ms). Route
to a replica only for use cases that tolerate this — dashboards, UIs, non-time-critical read
endpoints — and never for read-after-write patterns where a consumer must immediately see just-written
data. Using more than one read replica is currently discouraged, because replicas are not updated in
lockstep and can diverge from each other.

| Property                                    | Default       | Description                                        |
|---------------------------------------------|---------------|----------------------------------------------------|
| `jeap.datasource.replica.enabled`           | `false`       | Enable the read-replica datasource and routing     |
| `jeap.datasource.replica.url`               | inferred      | JDBC URL of the read-only endpoint                 |
| `jeap.datasource.replica.username`          | `{app}_db_ro` | Replica DB username                                |
| `jeap.datasource.replica.aws.hostname`      | —             | Read-only endpoint host (used when `url` unset)    |
| `jeap.datasource.replica.aws.port`          | `5432`        | Read-only endpoint port                            |
| `jeap.datasource.replica.aws.database-name` | from primary  | Replica database name; falls back to the primary's |
| `jeap.datasource.replica.hikari.*`          | see above     | HikariCP tuning for the replica pool               |

## Common patterns and pitfalls

- **`spring.datasource.*` is ignored** — always configure under `jeap.datasource.*`.
- **Token expiry** — keep Hikari `max-lifetime` below the 15-minute IAM token lifetime (the default
  840000 ms already does this); do not raise it carelessly.
- **Local H2** — set a password and `hikari.schema: PUBLIC` (plus the matching
  `hibernate.default_schema`), since the wrapper and IAM auth do not apply locally.
- **open-in-view** — Spring Boot enables `spring.jpa.open-in-view` by default, which keeps the first
  transaction open across a request; this can trip the read-only/read-write consistency check in
  `jeap-spring-boot-tx`. Annotate the entry method with `@Transactional` to mark the top-level
  transaction read-write when needed.

## Related

- [jeap-spring-boot-tx](jeap-spring-boot-tx.md)
- [jeap-spring-boot-vault-starter](jeap-spring-boot-vault-starter.md)
- [jeap-spring-boot-object-storage-starter](jeap-spring-boot-object-storage-starter.md)
- [Configuration property reference](configuration.md)
- [jeap-spring-boot-starters](../README.md)
