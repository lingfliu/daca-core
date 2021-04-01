package xyz.issc.daca;

import lombok.Data;
import xyz.issc.daca.spec.Code;

import java.util.List;
import java.util.Map;

@Data
public class FullMessage {
    Message meta;
    Message payload;
    String type;
    String name;

    public static FullMessage compose(Code metaCode, Code code, Map<String, Svo> attrs) {
        Message meta = new Message(metaCode);
        Message payload = new Message(code);

        List<String> metaAttrNames = metaCode.getAttrNames();

        for (String name : metaAttrNames) {
            if (attrs.containsKey(name)) {
               meta.data.put(name, attrs.get(name));
            }
        }

        List<String> attrNames = code.getAttrNames();
        for (String name : attrNames) {
            if (attrs.containsKey(name)) {
                payload.data.put(name, attrs.get(name));
            }
        }

        return new FullMessage(meta, payload);
    }

    public FullMessage(Message meta, Message payload) {
        this.meta = meta;
        this.payload = payload;
        type = payload.getType();
        name = payload.getName();
    }


    public Svo getAttrByName(String name) {
        if (meta.getData().containsKey(name)) {
            return meta.data.get(name);
        }
        if (payload.getData().containsKey(name)) {
            return payload.data.get(name);
        }
        return null;
    }
}
