package xyz.issc.daca;


import lombok.Getter;

public abstract class NioChannel {
    @Getter
    protected String addr;
    public abstract void close();
    public abstract int send(byte[] bytes);
}
