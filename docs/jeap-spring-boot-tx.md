# Transaction routing (tx)

`jeap-spring-boot-tx` provides read-replica-aware transaction routing for Spring's standard
transaction management. Together with the
[PostgreSQL AWS starter](jeap-spring-boot-postgresql-aws-starter.md) it lets explicitly marked
read-only transactions run against an AWS RDS read replica while all other transactions go to the
primary (writer), relieving the writer from read-heavy load. The module carries no AWS SDK
dependency, so the `@TransactionalReadReplica` annotation can be used in modules that should not
depend on the RDS starter directly.

Because the replica page cache is updated asynchronously, reads from a replica are only **eventually
consistent** — route there only when a short staleness window is acceptable (dashboards, UIs,
read-only consumers), and never for read-after-write patterns where the caller must see just-written
data.

## How it works

`JeapTxTransactionAutoConfig` registers two static bean post-processors:

- `ReadReplicaAwareTransactionManagerBeanPostProcessor` wraps the application's default
  `transactionManager` in a `ReadReplicaAwareTransactionManager` (with replica routing **off**) — so
  ordinary `@Transactional` work is unchanged but the consistency checks below still apply.
- `ReadReplicaTransactionManagerBeanDefinitionRegistryPostProcessor` makes a bean named
  `readReplicaTransactionManager` always available. When `jeap.datasource.replica.enabled=true` it
  registers a second `ReadReplicaAwareTransactionManager` with replica routing **on**; when the
  replica is disabled it simply **aliases** `readReplicaTransactionManager` to `transactionManager`,
  so the same annotations work transparently in non-replica (and local) environments.

`ReadReplicaAwareTransactionManager` is a wrapper around the real `PlatformTransactionManager`. On a
top-level transaction it records, in thread-locals, whether the transaction is read-only and whether
it should route to the replica. `ReadReplicaAwareTransactionRoutingDataSource` (an
`AbstractRoutingDataSource` configured by the PostgreSQL AWS starter) reads that flag and resolves
the lookup key to `reader` or `writer` accordingly. The routing decision is taken once, when the
top-level transaction is opened.

The manager also enforces consistency: it **forbids opening a read-write transaction nested inside a
read-only top-level transaction**, and (when routing to the replica) rejects a read-write top-level
transaction annotated for the replica. This fails fast — including in unit/integration tests — and
prevents two subtle bugs: Hibernate setting flush mode to `NEVER` (silently dropping writes) and a
read replica refusing writes at runtime.

Two Micrometer counters are exported: `jeap_db_transaction_readreplica` (transactions routed to a
read replica) and `jeap_db_transaction_rw` (writer-instance transactions).

## Using `@TransactionalReadReplica`

`@TransactionalReadReplica` is a meta-annotation over `@Transactional`, bound to
`readReplicaTransactionManager` with `readOnly = true`. Annotate a read-only method to route its
transaction to the replica (or to the writer if no replica is configured):

```java
import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;

@TransactionalReadReplica            // -> read replica if available, else writer
public List<Order> listOrders() {
    return orderRepository.findAll();
}

@Transactional                       // -> always the writer instance
public void placeOrder(Order order) {
    orderRepository.save(order);
}
```

For programmatic transaction management, inject the manager by name:

```java
@Autowired
@Qualifier("readReplicaTransactionManager")
PlatformTransactionManager readReplicaTransactionManager;

TransactionTemplate template = new TransactionTemplate(readReplicaTransactionManager);
```

The annotation forwards the usual `@Transactional` attributes (`propagation`, `isolation`, `timeout`,
`timeoutString`, `rollbackFor`, `rollbackForClassName`, `noRollbackFor`, `noRollbackForClassName`).

## Configuration

| Property                          | Default | Description                             |
|-----------------------------------|---------|-----------------------------------------|
| `jeap.datasource.replica.enabled` | `false` | Enable read-replica transaction routing |

## Common pitfalls

- **Nested read-write in read-only** — refactor so the top-level transaction matches the most
  permissive operation; the manager throws an `IllegalStateException` otherwise.
- **`spring.jpa.open-in-view`** — enabled by default in Spring Boot, it keeps the first transaction
  open across a web request and can trigger the consistency check for an unannotated read-only entry
  point; annotate the entry method with `@Transactional` to mark the top-level transaction read-write.
- **Stale reads** — only annotate methods whose reads tolerate replication lag.

## Related

- [jeap-spring-boot-postgresql-aws-starter](jeap-spring-boot-postgresql-aws-starter.md)
- [Configuration property reference](configuration.md)
- [jeap-spring-boot-starters](../README.md)
