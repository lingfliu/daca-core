package xyz.issc.daca;

import lombok.Data;
import xyz.issc.daca.spec.Code;

import java.util.HashMap;
import java.util.Map;

@Data
public class Message {
    String type;
    String name;
    Map<String, Svo> data; //name:value

    Map<String, Integer> attrLen = new HashMap<>();
    public void putAttrLen(String name, Integer attr) {
        attrLen.put(name, attr);
    }


    public Message() {
        data = new HashMap<>();
    }

    /**
     * create empty mesage body given code specification
     * @param code
     */
    public Message(Code code)  {
        data = new HashMap<>();
        type = code.getType();
        name = code.getName();
    }

    public void putAttr(String name, Svo attr) {
        data.put(name, attr);
    }
}
