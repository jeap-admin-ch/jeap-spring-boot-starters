package ch.admin.bit.jeap.config.client;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.core.env.Environment;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class JeapMessagingKafkaProperties extends JeapKafkaProperties {

    private static final String PREFIX = "jeap.messaging.kafka.";

    private String consumerBootstrapServers;
    private String producerBootstrapServers;
    private String adminClientBootstrapServers;

    static JeapMessagingKafkaProperties from(Environment environment) {
        JeapMessagingKafkaProperties props = new JeapMessagingKafkaProperties();
        props.initializeFrom(environment);
        props.consumerBootstrapServers = environment.getProperty(PREFIX + "consumer-bootstrap-servers");
        props.producerBootstrapServers = environment.getProperty(PREFIX + "producer-bootstrap-servers");
        props.adminClientBootstrapServers = environment.getProperty(PREFIX + "admin-client-bootstrap-servers");
        return props;
    }

    @Override
    protected String getPrefix() {
        return PREFIX;
    }

}

