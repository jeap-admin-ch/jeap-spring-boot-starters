package ch.admin.bit.jeap.log.syslog.test;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;

/**
 * Handler storing received data in a {@link StringBuilder}
 */
@RequiredArgsConstructor
class StringReceivingServerHandler extends ChannelInboundHandlerAdapter {
    private final StringBuilder receivedData;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            ByteBuf buf = (ByteBuf) msg;
            String str = buf.toString(StandardCharsets.UTF_8);
            synchronized (receivedData) {
                receivedData.append(str);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
        ctx.close();
    }
}
