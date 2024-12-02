package ch.admin.bit.jeap.log.syslog.connection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionStateTest {

    @Test
    void connectionState_basicStateHandling() {
        ConnectionState connectionState = ConnectionState.disconnected();
        assertFalse(connectionState.isConnected());

        connectionState.notifyConnected();
        assertTrue(connectionState.isConnected());

        connectionState.notifyDisconnected();
        assertFalse(connectionState.isConnected());
    }

    @Test
    void connectionState_shouldReconnectIfDisconnected() {
        ConnectionState connectionState = new ConnectionState();
        connectionState.notifyConnected();
        assertFalse(connectionState.shouldReconnect());

        connectionState.notifyDisconnected();
        assertTrue(connectionState.shouldReconnect());
    }

}
