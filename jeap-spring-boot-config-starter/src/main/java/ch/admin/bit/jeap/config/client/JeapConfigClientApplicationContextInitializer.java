package ch.admin.bit.jeap.config.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Apply the CloudBusKafkaConfigEnvPostProcessor after Spring Cloud Config added properties from a config server or
 * from a vault server to the environment. This application context initializer is needed because environment
 * post-processors registered with Spring Boot get executed before the Spring Cloud Config provided properties are available.
 * This application context initializer is executed after the Spring Cloud Config application context initializer
 * that adds the config server or vault properties to the environment.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 15) // lower than PropertySourceBootstrapConfiguration
public class JeapConfigClientApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Log LOG = LogFactory.getLog(JeapConfigClientApplicationContextInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        var cloudBusKafkaConfigEnvPostProcessorEnvPostProcessor = new CloudBusKafkaConfigEnvPostProcessor(LOG);
        cloudBusKafkaConfigEnvPostProcessorEnvPostProcessor.postProcessEnvironment(applicationContext.getEnvironment(), null);
    }

}
