package ch.admin.bit.jeap.db.tx.config;

import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
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
}
