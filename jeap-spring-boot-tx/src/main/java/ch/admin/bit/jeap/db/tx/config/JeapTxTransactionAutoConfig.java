package ch.admin.bit.jeap.db.tx.config;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;

@AutoConfiguration(after = TransactionAutoConfiguration.class)
public class JeapTxTransactionAutoConfig {

    private JeapTxTransactionAutoConfig() {
    }

    /**
     * Must be static! Otherwise, the whole autoconfiguration class is eagerly instantiated when running bean post
     * processors early in the spring context lifecycle. Avoids the dreaded "Bean is not eligible for getting processed
     * by all BeanPostProcessors (for example: not eligible for auto-proxying)" warning.
     */
    @Bean
    public static ReadReplicaAwareTransactionManagerBeanPostProcessor platformTransactionManagerBeanPostProcessor() {
        return new ReadReplicaAwareTransactionManagerBeanPostProcessor();
    }

    /**
     * Must be static - see above
     */
    @Bean
    public static BeanDefinitionRegistryPostProcessor readReplicaTransactionManagerBeanDefinitionRegistryPostProcessor() {
        return new ReadReplicaTransactionManagerBeanDefinitionRegistryPostProcessor();
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    @ConditionalOnBean({PlatformTransactionManager.class, TransactionAttributeSource.class})
    static AwsJdbcFailoverRetryAdvisor awsJdbcFailoverRetryAdvisor(
            ListableBeanFactory beanFactory,
            TransactionAttributeSource transactionAttributeSource,
            Environment environment,
            @Qualifier("transactionInterceptor") ObjectProvider<TransactionInterceptor> transactionInterceptorProvider) {
        TransactionInterceptor transactionInterceptor = transactionInterceptorProvider.getIfAvailable();
        TransactionManager defaultTransactionManager =
                transactionInterceptor == null ? null : transactionInterceptor.getTransactionManager();
        AwsJdbcFailoverRetryProperties properties = Binder.get(environment).bindOrCreate(
                "jeap.datasource.aws.failover-retry", Bindable.of(AwsJdbcFailoverRetryProperties.class));
        return new AwsJdbcFailoverRetryAdvisor(
                beanFactory, transactionAttributeSource, defaultTransactionManager, properties);
    }
}
