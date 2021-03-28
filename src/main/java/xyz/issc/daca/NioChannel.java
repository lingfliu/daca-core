package xyz.issc.daca;

public abstract class NioChannel {
    public abstract String getAddr();
    public abstract void close();
    public abstract int send(byte[] bytes);
}
