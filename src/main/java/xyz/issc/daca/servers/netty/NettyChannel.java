package xyz.issc.daca.servers.netty;

import io.netty.channel.Channel;
import xyz.issc.daca.NioChannel;

public class NettyChannel extends NioChannel {

    Channel chn;

    public NettyChannel(Channel chn) {
        this.chn = chn;
        this.addr = chn.remoteAddress().toString().split(":")[0];
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
