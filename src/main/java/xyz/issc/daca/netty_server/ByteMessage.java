package xyz.issc.daca.netty_server;

public class ByteMessage {
    byte[] bytes;
    int len;

    public ByteMessage(byte[] bytes, int len) {
        this.bytes = bytes;
        this.len = len;
    }
}
