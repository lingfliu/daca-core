package xyz.issc.daca;

import lombok.Data;

@Data
public abstract class NioServer {
    AppConnManager appConnManager;


    public interface EventListener {
        void onChannelStateChanged(String addr, int state);
        void onChannelConnected(NioChannel channel);
        void onChannelDisconnected(NioChannel channel);
        void onReceived(String addr, byte[] bytes, int len);
        void onSent(String addr, byte[] bytes, int len);
    }

    EventListener eventListener;

    public abstract void start();
    public abstract void shutdown();
}

