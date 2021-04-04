package xyz.issc.daca.servers.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class NettyByteDispatcher extends ChannelInboundHandlerAdapter {


    public interface ChannelListener {
        void onReceiveBytes(String addr, byte[] bytes, int len);
    }
    ChannelListener channelListener;

    public NettyByteDispatcher(ChannelListener channelListener) {
        this.channelListener = channelListener;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = ctx.channel();
        String addr = channel.remoteAddress().toString();
        ByteMessage bMsg = (ByteMessage)  msg;
        if (channelListener != null) {
            channelListener.onReceiveBytes(addr, bMsg.bytes, bMsg.len);
        }

    }
}
