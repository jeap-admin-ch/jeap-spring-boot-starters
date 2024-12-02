package ch.admin.bit.jeap.db.tx.config;

import ch.admin.bit.jeap.db.tx.ReadReplicaAwareTransactionManager;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@RequiredArgsConstructor
public class ReadReplicaAwareTransactionManagerBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

    private static final String TRANSACTION_MANAGER = "transactionManager";

    private BeanFactory beanFactory;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (TRANSACTION_MANAGER.equals(beanName) && PlatformTransactionManager.class.isAssignableFrom(bean.getClass())) {
            log.info("Replacing the transactionManager with a ReadReplicaAwareTransactionManager");
            boolean routeTransactionsToReadReplica = false; // The primary transactionManager does not route transactions to read replicas
            PlatformTransactionManager delegate = (PlatformTransactionManager) bean;
            return new ReadReplicaAwareTransactionManager(delegate, routeTransactionsToReadReplica, () -> (MeterRegistry) beanFactory.getBean(MeterRegistry.class));
        }
        return bean;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
