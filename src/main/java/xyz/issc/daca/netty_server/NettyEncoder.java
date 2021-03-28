package xyz.issc.daca.netty_server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class NettyEncoder extends MessageToByteEncoder<ByteMessage> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, ByteMessage byteMessage, ByteBuf byteBuf) throws Exception {
        ByteMessage msg = (ByteMessage) byteMessage;
        byteBuf.writeBytes(msg.bytes, 0, msg.len);
    }
}
