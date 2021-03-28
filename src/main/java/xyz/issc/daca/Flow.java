package xyz.issc.daca;

import lombok.Data;

@Data
public class Flow {
    public static final int UPLINK = 0;
    public static final int DOWNLINK = 1;
    FullMessage message;
    long formedAt;
    int direction;


    public boolean contain(String name, Svo ref) {
        Svo val = message.getAttrByName(name);
        if (val != null && ref.equals(val)) {
            return true;
        }
        else {
            return false;
        }
    }
}
