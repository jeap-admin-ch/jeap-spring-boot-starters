package ch.admin.bit.jeap.log.syslog;

import ch.admin.bit.jeap.log.syslog.connection.TLSSyslogConnection;
import ch.admin.bit.jeap.log.syslog.connection.TLSSyslogConnectionProperties;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.net.ssl.SSLComponent;
import ch.qos.logback.core.net.ssl.SSLConfiguration;
import ch.qos.logback.core.spi.ContextAware;
import lombok.Getter;
import lombok.Setter;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * A logback appender sending messages using a {@link TLSSyslogConnection} via TCP/SSL to a syslog server. The
 * {@link #setEncoder(Encoder) encoder} is responsible for providing the message payload understood by the receiving
 * syslog server. The appender makes an effort to retry failed log submissions and to reconnect broken connections. In
 * the end, however, log submission in a distributed system is best-effort and can be lossy if networking issues arise.
 *
 */
public class TLSSyslogAppender extends AppenderBase<ILoggingEvent> implements SSLComponent {

    private static final int DEFAULT_PORT = 6514;
    private static final int DEFAULT_TIMEOUT_MILLIS = 5000;
    private static final int DEFAULT_TCP_KEEPALIVE_IDLE_SECONDS = 20;
    private static final int MAX_MESSAGE_SIZE_LIMIT = 65000;

    @Setter
    private String syslogHost;
    @Setter
    private int port = DEFAULT_PORT;
    @Setter
    private int timeoutMillis = DEFAULT_TIMEOUT_MILLIS;
    @Setter
    private int tcpKeepIdleSeconds = DEFAULT_TCP_KEEPALIVE_IDLE_SECONDS;
    @Setter
    private int maxMessageSize = MAX_MESSAGE_SIZE_LIMIT;
    @Setter
    private Encoder<ILoggingEvent> encoder;
    @Setter
    @Getter
    private SSLConfiguration ssl = new SSLConfiguration();

    private TLSSyslogConnection syslogConnection;

    private static final Map<String, TLSSyslogConnection> CONNECTIONS = new ConcurrentHashMap<>();

    @Override
    protected void append(ILoggingEvent eventObject) {
        byte[] syslogMessage = encoder.encode(eventObject);
        transmit(syslogMessage);
    }

    protected void transmit(byte[] syslogMessage) {
        byte[] maxLengthSyslogMessage = syslogMessage;
        if (syslogMessage.length > maxMessageSize) {
            maxLengthSyslogMessage = Arrays.copyOfRange(syslogMessage, 0, maxMessageSize);
        }
        syslogConnection.transmit(maxLengthSyslogMessage);
    }

    @Override
    public void start() {
        if (syslogHost == null) {
            throw new IllegalArgumentException("syslogHost must be configured for " + getClass().getSimpleName());
        }
        if (encoder == null) {
            throw new IllegalArgumentException("an encoder must be configured for " + getClass().getSimpleName());
        }

        try {
            ContextAware contextAware = this;
            syslogConnection = createOrReuseSyslogConnection(contextAware);
            super.start();
        } catch (Exception e) {
            addError(format("Error starting %s using syslog host %s:%d", getClass().getSimpleName(), syslogHost, port), e);
        }
    }

    /**
     * Connections to a syslog host are re-used across appender restarts. In Spring Boot with spring cloud, the appender
     * is created at least twice: Once when the app starts, then upon context refresh after the bootstrap phase. The
     * syslog connection would thus be created twice in a very short amount of time and is cached. That boot speeds up
     * app startup and lowers the connection load on the syslog host.
     */
    @SuppressWarnings("java:S106") // deliberately using System.out here as the logger is not yet initialized
    private TLSSyslogConnection createOrReuseSyslogConnection(ContextAware contextAware) throws GeneralSecurityException {
        String syslogHostPortKey = getName() + ":" + syslogHost + ":" + port;
        TLSSyslogConnection existingConnection = CONNECTIONS.get(syslogHostPortKey);
        if (existingConnection != null) {
            System.out.println("Reusing syslog connection to " + syslogHostPortKey);
            return existingConnection;
        }
        TLSSyslogConnection newConnection = connectToSyslog(contextAware);
        CONNECTIONS.put(syslogHostPortKey, newConnection);
        return newConnection;
    }

    private TLSSyslogConnection connectToSyslog(ContextAware contextAware) throws GeneralSecurityException {
        TLSSyslogConnection newConnection = createSyslogConnection(contextAware);
        // This will not fail if unable to connect - avoids blocking the application from starting if the syslog
        // host is not available at the moment
        if (!newConnection.attemptConnection()) {
            // Retry to reconnect once immediately in case of failure
            newConnection.attemptConnection();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(newConnection::disconnect));
        return newConnection;
    }

    protected TLSSyslogConnection createSyslogConnection(ContextAware contextAware) throws GeneralSecurityException {
        TLSSyslogConnectionProperties props = TLSSyslogConnectionProperties.builder()
                .syslogHost(syslogHost)
                .port(port)
                .ssl(getSsl())
                .timeoutMillis(timeoutMillis)
                .tcpKeepIdleSeconds(tcpKeepIdleSeconds)
                .build();
        return TLSSyslogConnection.create(contextAware, props);
    }

    static void cleanConnectionCache() {
        CONNECTIONS.values().forEach(TLSSyslogConnection::disconnect);
        CONNECTIONS.clear();
    }
}
