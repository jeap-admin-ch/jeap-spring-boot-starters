package ch.admin.bit.jeap.config.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.core.env.Environment;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class JeapCloudBusKafkaProperties extends JeapKafkaProperties {

    static JeapCloudBusKafkaProperties from(Environment environment) {
        JeapCloudBusKafkaProperties props = new JeapCloudBusKafkaProperties();
        props.initializeFrom(environment);
        return props;
    }

    @Override
    protected String getPrefix() {
        return "jeap.config.client.cloud.bus.kafka.";
    }

}

