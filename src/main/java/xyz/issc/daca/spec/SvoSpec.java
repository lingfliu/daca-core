package xyz.issc.daca.spec;

import lombok.Data;
import xyz.issc.daca.Svo;

@Data
public class SvoSpec {
    Svo.Type type;

    //effective for int, float
    Svo max;
    Svo min;

    //spec if is fixed content
    boolean isFixed;
    Svo content;

    public boolean check(Svo attr) {
        try {
            if (!isFixed) {
                if (attr.getType() == Svo.Type.INT) {
                    return attr.unpackInteger() >= min.unpackInteger() && attr.unpackInteger() <= max.unpackInteger();
                }

                if (attr.getType() == Svo.Type.FLOAT) {
                    return attr.unpackFloat() >= min.unpackFloat() && attr.unpackFloat() <= max.unpackFloat();
                }

                //by default always pass the check
                return true;
            } else {
                return content.equals(attr);
            }
        } catch (Svo.ValueUnpackException e) {
            e.printStackTrace();
            return false;
        }
    }
}
