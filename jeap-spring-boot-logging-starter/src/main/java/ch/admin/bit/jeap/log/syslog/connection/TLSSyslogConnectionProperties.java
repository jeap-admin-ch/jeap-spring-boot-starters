package ch.admin.bit.jeap.log.syslog.connection;

import ch.qos.logback.core.net.ssl.SSLConfiguration;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Builder
@Value
public class TLSSyslogConnectionProperties {
    @NonNull
    String syslogHost;
    @NonNull
    Integer port;
    @NonNull
    Integer timeoutMillis;
    @NonNull
    Integer tcpKeepIdleSeconds;
    @NonNull
    SSLConfiguration ssl;

    @Builder.Default
    boolean debugMode = Boolean.getBoolean("jeap.logging.logrelay.debug");

}
