package ch.admin.bit.jeap.log.syslog.test;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.security.KeyStore;

/**
 * Simple https server
 */
@RequiredArgsConstructor
class NettyHttpsServer {

    private final int port;
    private ChannelFuture channelFuture;
    private EventLoopGroup eventLoop;
    private EventLoopGroup workerLoop;

    @SneakyThrows
    void start(final ChannelHandler channelHandler) {
        char[] passphrase = "secret".toCharArray();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        KeyStore ks = KeyStore.getInstance("JKS");

        ks.load(getClass().getResourceAsStream("/testkeys.jks"), passphrase);
        kmf.init(ks, passphrase);
        sslContext.init(kmf.getKeyManagers(), null, null);
        SSLEngine engine = sslContext.createSSLEngine();
        engine.setUseClientMode(false);

        eventLoop = new NioEventLoopGroup();
        workerLoop = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(eventLoop, workerLoop)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new SslHandler(engine))
                                .addLast(channelHandler);
                    }
                });

        // Bind and start to accept incoming connections.
        channelFuture = b.bind(port).sync();

        System.out.println("Netty server started on port " + port);
    }

    @SneakyThrows
    void stop() {
        System.out.println("Netty server shutting down");
        workerLoop.shutdownGracefully();
        eventLoop.shutdownGracefully();
        // Wait for shutdown to complete
        channelFuture.channel().closeFuture().sync();
    }
}
