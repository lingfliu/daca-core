package xyz.issc.daca.servers.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyConnectionHandler extends ChannelInboundHandlerAdapter {
    Logger log = LoggerFactory.getLogger("netty");

    public interface ConnectionListener {
        void onChannelConnected(Channel channel);
        void onChannelDisconnected(Channel channel);
    }
    ConnectionListener connectionListener;

    public NettyConnectionHandler(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
//        log.info("channel connected, addr = " + ctx.channel().remoteAddress().toString());
        if (connectionListener != null) {
            connectionListener.onChannelConnected(ctx.channel());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        if (connectionListener != null) {
            connectionListener.onChannelDisconnected(ctx.channel());
        }
    }

}
