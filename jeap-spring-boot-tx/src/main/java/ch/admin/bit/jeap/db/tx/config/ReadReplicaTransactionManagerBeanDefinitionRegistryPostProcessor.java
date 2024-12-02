package ch.admin.bit.jeap.db.tx.config;

import ch.admin.bit.jeap.db.tx.ReadReplicaAwareTransactionManager;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * This class is responsible for registering the read replica transaction manager bean definition if the read replica
 * feature is enabled. If the feature is disabled, the transaction manager bean definition is aliased to the read replica
 * transaction manager bean definition to make sure that a transaction manager with the name "readReplicaTransactionManager"
 * is always available (referenced in @{@link ch.admin.bit.jeap.db.tx.TransactionalReadReplica}).
 */
public class ReadReplicaTransactionManagerBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, BeanFactoryAware, EnvironmentAware {
    private static final String READ_REPLICA_TRANSACTION_MANAGER = "readReplicaTransactionManager";
    private static final String TRANSACTION_MANAGER = "transactionManager";

    private BeanFactory beanFactory;
    private Environment environment;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        boolean readReplicaActive = environment.getProperty("jeap.datasource.replica.enabled", Boolean.class, false);
        if (readReplicaActive) {
            registry.getBeanDefinition(TRANSACTION_MANAGER)
                    .setPrimary(true);

            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(ReadReplicaAwareTransactionManager.class);
            beanDefinition.setPrimary(false);
            beanDefinition.setDependsOn(TRANSACTION_MANAGER);
            beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_NO);
            beanDefinition.setInstanceSupplier(this::getTransactionManager);
            registry.registerBeanDefinition(READ_REPLICA_TRANSACTION_MANAGER, beanDefinition);
        } else {
            registry.registerAlias(TRANSACTION_MANAGER, READ_REPLICA_TRANSACTION_MANAGER);
        }
    }

    private ReadReplicaAwareTransactionManager getTransactionManager() {
        PlatformTransactionManager delegate = (PlatformTransactionManager) beanFactory.getBean(TRANSACTION_MANAGER);
        return new ReadReplicaAwareTransactionManager(delegate, true, () -> (MeterRegistry) beanFactory.getBean(MeterRegistry.class));
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
