package ch.admin.bit.jeap.log.syslog.test;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class MockSslServer {

    private final StringBuilder receivedData = new StringBuilder();
    private NettyHttpsServer server;

    public void start(int port) {
        server = new NettyHttpsServer(port);
        server.start(new StringReceivingServerHandler(receivedData));
    }

    public void stop() {
        try {
            if (server != null) {
                server.stop();
            }
        } finally {
            server = null;
        }
    }

    public String getReceivedData() {
        synchronized (receivedData) {
            return receivedData.toString();
        }
    }

    public List<String> getReceivedLines() {
        return Stream.of(getReceivedData().split("\n"))
                .collect(toList());
    }
}
