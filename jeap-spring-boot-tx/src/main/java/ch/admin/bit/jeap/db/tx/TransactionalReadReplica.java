package ch.admin.bit.jeap.db.tx;

import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for {@link Transactional @Transactional} that indicates that a read-only transaction should be
 * started on a read-only replica instance of a database.
 * Note that read replicas might offer only eventual consistency, which means that the data might not yet be up-to-date
 * when reading from a read replica. I.e. AWS RDS Read Replicas might have a replication lag of a few milliseconds until
 * their page cache is up-to-date compared to the primary instance.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Transactional(
        transactionManager = "readReplicaTransactionManager",
        readOnly = true
)
public @interface TransactionalReadReplica {

    // Note: The following properties are copied from the @Transactional annotation to ensure that they can be set
    // on the @TransactionalReadReplica annotation as well. This is necessary because @TransactionalReadReplica
    // is a meta-annotation for @Transactional. Any properties set here will be applied to the @Transactional annotation.

    /**
     * The transaction propagation type.
     * <p>Defaults to {@link Propagation#REQUIRED}.
     *
     * @see org.springframework.transaction.interceptor.TransactionAttribute#getPropagationBehavior()
     */
    @AliasFor(annotation = Transactional.class)
    Propagation propagation() default Propagation.REQUIRED;

    /**
     * The transaction isolation level.
     * <p>Defaults to {@link Isolation#DEFAULT}.
     * <p>Exclusively designed for use with {@link Propagation#REQUIRED} or
     * {@link Propagation#REQUIRES_NEW} since it only applies to newly started
     * transactions. Consider switching the "validateExistingTransactions" flag to
     * "true" on your transaction manager if you'd like isolation level declarations
     * to get rejected when participating in an existing transaction with a different
     * isolation level.
     *
     * @see org.springframework.transaction.interceptor.TransactionAttribute#getIsolationLevel()
     * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setValidateExistingTransaction
     */
    @AliasFor(annotation = Transactional.class)
    Isolation isolation() default Isolation.DEFAULT;

    /**
     * The timeout for this transaction (in seconds).
     * <p>Defaults to the default timeout of the underlying transaction system.
     * <p>Exclusively designed for use with {@link Propagation#REQUIRED} or
     * {@link Propagation#REQUIRES_NEW} since it only applies to newly started
     * transactions.
     *
     * @return the timeout in seconds
     * @see org.springframework.transaction.interceptor.TransactionAttribute#getTimeout()
     */
    @AliasFor(annotation = Transactional.class)
    int timeout() default TransactionDefinition.TIMEOUT_DEFAULT;

    /**
     * The timeout for this transaction (in seconds).
     * <p>Defaults to the default timeout of the underlying transaction system.
     * <p>Exclusively designed for use with {@link Propagation#REQUIRED} or
     * {@link Propagation#REQUIRES_NEW} since it only applies to newly started
     * transactions.
     *
     * @return the timeout in seconds as a String value, e.g. a placeholder
     * @see org.springframework.transaction.interceptor.TransactionAttribute#getTimeout()
     * @since 5.3
     */
    @AliasFor(annotation = Transactional.class)
    String timeoutString() default "";

    /**
     * Defines zero (0) or more exception {@linkplain Class types}, which must be
     * subclasses of {@link Throwable}, indicating which exception types must cause
     * a transaction rollback.
     * <p>By default, a transaction will be rolled back on {@link RuntimeException}
     * and {@link Error} but not on checked exceptions (business exceptions). See
     * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)}
     * for a detailed explanation.
     * <p>This is the preferred way to construct a rollback rule (in contrast to
     * {@link #rollbackForClassName}), matching the exception type and its subclasses
     * in a type-safe manner. See the {@linkplain Transactional class-level javadocs}
     * for further details on rollback rule semantics.
     *
     * @see #rollbackForClassName
     * @see org.springframework.transaction.interceptor.RollbackRuleAttribute#RollbackRuleAttribute(Class)
     * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
     */
    @AliasFor(annotation = Transactional.class)
    Class<? extends Throwable>[] rollbackFor() default {};

    /**
     * Defines zero (0) or more exception name patterns (for exceptions which must be a
     * subclass of {@link Throwable}), indicating which exception types must cause
     * a transaction rollback.
     * <p>See the {@linkplain Transactional class-level javadocs} for further details
     * on rollback rule semantics, patterns, and warnings regarding possible
     * unintentional matches.
     *
     * @see #rollbackFor
     * @see org.springframework.transaction.interceptor.RollbackRuleAttribute#RollbackRuleAttribute(String)
     * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
     */
    @AliasFor(annotation = Transactional.class)
    String[] rollbackForClassName() default {};

    /**
     * Defines zero (0) or more exception {@link Class types}, which must be
     * subclasses of {@link Throwable}, indicating which exception types must
     * <b>not</b> cause a transaction rollback.
     * <p>This is the preferred way to construct a rollback rule (in contrast to
     * {@link #noRollbackForClassName}), matching the exception type and its subclasses
     * in a type-safe manner. See the {@linkplain Transactional class-level javadocs}
     * for further details on rollback rule semantics.
     *
     * @see #noRollbackForClassName
     * @see org.springframework.transaction.interceptor.NoRollbackRuleAttribute#NoRollbackRuleAttribute(Class)
     * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
     */
    @AliasFor(annotation = Transactional.class)
    Class<? extends Throwable>[] noRollbackFor() default {};

    /**
     * Defines zero (0) or more exception name patterns (for exceptions which must be a
     * subclass of {@link Throwable}) indicating which exception types must <b>not</b>
     * cause a transaction rollback.
     * <p>See the {@linkplain Transactional class-level javadocs} for further details
     * on rollback rule semantics, patterns, and warnings regarding possible
     * unintentional matches.
     *
     * @see #noRollbackFor
     * @see org.springframework.transaction.interceptor.NoRollbackRuleAttribute#NoRollbackRuleAttribute(String)
     * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
     */
    @AliasFor(annotation = Transactional.class)
    String[] noRollbackForClassName() default {};
}
