package xyz.issc.daca.netty_server;

import io.netty.channel.Channel;
import xyz.issc.daca.NioChannel;

public class NettyChannel extends NioChannel {

    Channel chn;

    public NettyChannel(Channel chn) {
        this.chn = chn;
    }

    @Override
    public String getAddr() {
        return chn.remoteAddress().toString();
    }

    @Override
    public void close() {
        chn.close();
    }

    @Override
    public int send(byte[] bytes) {
        chn.writeAndFlush(new ByteMessage(bytes, bytes.length));
        return bytes.length;
    }
}
