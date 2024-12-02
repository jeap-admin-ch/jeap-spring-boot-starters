package ch.admin.bit.jeap.config.client;

import lombok.Getter;
import org.springframework.core.env.Environment;

@Getter
abstract class JeapKafkaProperties {

    private String bootstrapServers;
    private String securityProtocol;
    private String username;
    private String password;

    protected void initializeFrom(Environment environment) {
        this.bootstrapServers = environment.getProperty(getPrefix() + "bootstrap-servers");
        this.securityProtocol = environment.getProperty(getPrefix() + "security-protocol");
        this.username = environment.getProperty(getPrefix() + "username");
        this.password= environment.getProperty(getPrefix() + "password");
    }

    boolean isConfigured() {
        return bootstrapServers != null;
    }

    boolean isSsl() {
        if (securityProtocol == null) {
            return false;
        } else {
            return securityProtocol.endsWith("SSL");
        }
    }

    boolean isSasl() {
        if (securityProtocol == null) {
            return false;
        } else {
            return securityProtocol.startsWith("SASL");
        }
    }

    abstract protected String getPrefix();

}

