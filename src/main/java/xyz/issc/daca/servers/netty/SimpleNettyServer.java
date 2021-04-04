package xyz.issc.daca.servers.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import xyz.issc.daca.Aconn;
import xyz.issc.daca.NioServer;

public class SimpleNettyServer extends NioServer {

    public static class Builder {
        private int maxUser = -1;
        private int port = -1;

        public Builder maxUser (int val) {
            this.maxUser = val;
            return this;
        }

        public Builder port(int val) {
            this.port = val;
            return this;
        }
        public SimpleNettyServer build() {
            if (maxUser <= 0 || port < 1024) {
                return null;
            }
            else {
                return new SimpleNettyServer(maxUser, port);
            }
        }
    }

    private int maxUser;
    private int port;
    private ChannelFuture channel;

    private ServerBootstrap bootstrap;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;

    private SimpleNettyServer(int maxUser, int port) {
        this.maxUser = maxUser;
        this.port = port;
    }


    @Override
    public void start() {
        try {
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();

            bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, maxUser)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                            ch.pipeline().addLast(new NettyDecoder());
                            ch.pipeline().addLast(new NettyByteDispatcher((addr, bytes, len) -> {
                                if (getEventListener() != null) {

                                    getEventListener().onReceived(addr.split(":")[0], bytes, len);
                                }
                            }));
                            ch.pipeline().addLast(new NettyConnectionHandler(new NettyConnectionHandler.ConnectionListener() {
                                @Override
                                public void onChannelConnected(Channel channel) {
                                    if (getEventListener() != null) {
                                        getEventListener().onChannelConnected(new NettyChannel(channel));
                                    }
                                }

                                @Override
                                public void onChannelDisconnected(Channel channel) {
                                    if (getEventListener() != null) {
                                        getEventListener().onChannelStateChanged(channel.remoteAddress().toString(), Aconn.STATE_DISCONNECTED);
                                    }
                                }
                            }));
                            ch.pipeline().addFirst(new NettyEncoder());
                        }
                    });
            channel = bootstrap.bind(port);
            channel.sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
