package ch.admin.bit.jeap.db.tx.config;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@AutoConfiguration(after = TransactionAutoConfiguration.class)
public class JeapTxTransactionAutoConfig {

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
    @ConditionalOnBean({PlatformTransactionManager.class, TransactionAttributeSource.class})
    AwsJdbcFailoverRetryAspect awsJdbcFailoverRetryAspect(
            ListableBeanFactory beanFactory,
            TransactionAttributeSource transactionAttributeSource,
            @Qualifier("transactionInterceptor") ObjectProvider<TransactionInterceptor> transactionInterceptorProvider) {
        TransactionInterceptor transactionInterceptor = transactionInterceptorProvider.getIfAvailable();
        TransactionManager defaultTransactionManager =
                transactionInterceptor == null ? null : transactionInterceptor.getTransactionManager();
        return new AwsJdbcFailoverRetryAspect(beanFactory, transactionAttributeSource, defaultTransactionManager);
    }
}
