package ch.admin.bit.jeap.log.syslog.connection;

class ConnectionState {

    private boolean connected = false;

    static ConnectionState disconnected() {
        return new ConnectionState();
    }

    void notifyConnected() {
        connected = true;
    }

    void notifyDisconnected() {
        connected = false;
    }

    boolean shouldReconnect() {
        return !connected;
    }

    public boolean isConnected() {
        return this.connected;
    }
}
