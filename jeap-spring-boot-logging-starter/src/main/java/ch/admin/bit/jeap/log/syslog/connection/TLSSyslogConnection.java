package ch.admin.bit.jeap.log.syslog.connection;

import ch.admin.bit.jeap.log.metrics.LoggingMetrics;
import ch.qos.logback.core.net.ssl.SSLConfigurableSocket;
import ch.qos.logback.core.net.ssl.SSLParametersConfiguration;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.util.CloseUtil;
import jdk.net.ExtendedSocketOptions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;

@RequiredArgsConstructor
@SuppressWarnings("java:S106") // deliberately using System.out/err here as the logger is not yet initialized
public class TLSSyslogConnection {

    private final SSLContext sslContext;
    private final SSLParametersConfiguration sslParameters;
    private final TLSSyslogConnectionProperties props;

    private final ConnectionState connectionState = ConnectionState.disconnected();
    private boolean connectionLoggedOnce = false;
    private SSLSocket clientSocket;

    @Getter
    private String lastTransmitError;

    public static TLSSyslogConnection create(ContextAware context, TLSSyslogConnectionProperties props) throws GeneralSecurityException {
        SSLContext sslContext = props.getSsl().createContext(context);
        SSLParametersConfiguration parameters = props.getSsl().getParameters();
        parameters.setContext(context.getContext());
        return new TLSSyslogConnection(sslContext, parameters, props);
    }

    /**
     * Writes the message to the TCP socket's write buffer, attempting to reconnect if necessary
     */
    public void transmit(byte[] syslogMessage) {
        if (connectionState.shouldReconnect()) {
            attemptConnection();
        }
        if (!connectionState.isConnected()) {
            debug("not transmitting - not connected");
        }

        boolean success = attemptTransmit(syslogMessage);
        // Immediate single retry on error. Monitoring has shown that most transmit errors are due to 'broken pipe'
        // errors, which means the TCP connection has been lost/reset. Connection errors do not occur however, which
        // means that transmit errors can usually be fixed by re-establishing the TCP connection.
        if (!success) {
            debug("transmit failed - retrying");
            disconnect();
            attemptConnection();
            success = attemptTransmit(syslogMessage);
            // If the message still cannot be transmitted, try to re-connect on the next statement.
            if (!success) {
                disconnect();
            }
        }
    }

    private boolean attemptTransmit(byte[] syslogMessage) {
        debug("attempt transmit");
        long start = System.nanoTime();
        try {
            OutputStream outputStream = clientSocket.getOutputStream();
            outputStream.write(syslogMessage);
            outputStream.flush();
            debug("transmit successful");
            return true;
        } catch (IOException ex) {
            debug("transmit failed", ex.getMessage());
            lastTransmitError = ex.getMessage();
            LoggingMetrics.incrementDistributedLogTransmitError();
            return false;
        } finally {
            LoggingMetrics.distributedLogTransmitTime(Duration.ofNanos(System.nanoTime() - start));
        }
    }

    /**
     * Attempts to connect, initializes the {@link #clientSocket} connected to the syslog server and sets the connection
     * state to connected.
     * <p>
     * If the connection attempt is unsuccessful, the error is logged once per application run to stderr, and the
     * connection is left in disconnected state. No exception is thrown in this case to allow for a reconnection
     * attempt later without blocking application startup.
     */
    public boolean attemptConnection() {
        long start = System.currentTimeMillis();
        try {
            SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(); // NOSONAR close is handled in disconnect()
            sslSocket.setUseClientMode(true);
            // Read timeout is used during the SSL handshake
            sslSocket.setSoTimeout(props.getTimeoutMillis());
            sslParameters.configure(new SSLConfigurableSocket(sslSocket));
            sslSocket.connect(new InetSocketAddress(props.getSyslogHost(), props.getPort()), props.getTimeoutMillis());
            sslSocket.setTcpNoDelay(true);
            sslSocket.setKeepAlive(true);
            // Keep-Alive idle time - seconds of idle time before keep-alive initiates a probe
            // If set higher than the logrelay's idle connection reset timeout (2-3min), the connection will be reset often,
            // with a high probability of loosing log messages
            sslSocket.setOption(ExtendedSocketOptions.TCP_KEEPIDLE, props.getTcpKeepIdleSeconds());
            // Keep-Alive retry maximum limit
            sslSocket.setOption(ExtendedSocketOptions.TCP_KEEPCOUNT, 2);
            // Keep-Alive retransmission interval time - seconds to wait before retransmitting keep-alive probe
            sslSocket.setOption(ExtendedSocketOptions.TCP_KEEPINTERVAL, 5);
            sslSocket.startHandshake();
            this.clientSocket = sslSocket;
            onConnectionSuccesful(System.currentTimeMillis() - start);
            return true;
        } catch (Exception ex) {
            disconnect();
            onConnectionError(ex);
            return false;
        }
    }

    public void disconnect() {
        debug("disconnect");
        connectionState.notifyDisconnected();
        CloseUtil.closeQuietly(clientSocket);
        clientSocket = null;
    }

    private void onConnectionSuccesful(long connectionElapsedTimeMs) {
        connectionState.notifyConnected();
        LoggingMetrics.incrementDistributedLogConnectionEstablished();
        if (!connectionLoggedOnce) {
            connectionLoggedOnce = true;
            System.out.printf("TLS Syslog Appender connected to %s:%d in %dms%n",
                    props.getSyslogHost(), props.getPort(), connectionElapsedTimeMs);
        }
    }

    private void onConnectionError(Exception ex) {
        debug("connection not succesful", ex.getMessage());
        LoggingMetrics.incrementDistributedLogConnectionError();
        if (!connectionLoggedOnce) {
            connectionLoggedOnce = true;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ex.printStackTrace(new PrintStream(baos, true, StandardCharsets.UTF_8));
            System.out.printf("TLS Syslog Appender failed to connect to %s:%d: %s%n",
                    props.getSyslogHost(), props.getPort(), baos.toString(StandardCharsets.UTF_8));
        }
    }

    private void debug(String msg) {
        debug(msg, null);
    }

    private void debug(String msg, Object details) {
        if (props.isDebugMode()) {
            System.err.println("[syslog] " + msg + (details == null ? "" : " " + details));
        }
    }
}
